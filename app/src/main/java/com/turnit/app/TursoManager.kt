package com.turnit.app

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class TursoManager(private val dbUrl: String, private val authToken: String) {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun signup(username: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val id = UUID.randomUUID().toString()
        val sql = "INSERT INTO users (id, username, email, password_hash) VALUES ('$id', '$username', '$email', '$pass');"
        execute(sql) { success, _, errorMsg -> 
            onResult(success, if (success) id else "Signup Fail: $errorMsg") 
        }
    }

    fun login(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val sql = "SELECT id FROM users WHERE username = '$username' AND password_hash = '$pass';"
        execute(sql) { success, data, errorMsg ->
            try {
                if (success && data != null) {
                    val rows = data.getJSONArray("rows")
                    if (rows.length() > 0) onResult(true, rows.getJSONArray(0).getString(0))
                    else onResult(false, "Invalid Credentials")
                } else {
                    onResult(false, "Login Fail: $errorMsg")
                }
            } catch (e: Exception) {
                onResult(false, "Parse Error: ${e.message}")
            }
        }
    }

    private fun execute(sql: String, callback: (Boolean, JSONObject?, String?) -> Unit) {
        // Diagnostic 1: Check Secrets Injection
        if (dbUrl.isBlank()) { callback(false, null, "TURSO_URL secret is blank"); return }
        if (!dbUrl.startsWith("http")) { callback(false, null, "TURSO_URL must start with https://"); return }
        if (authToken.isBlank()) { callback(false, null, "TURSO_TOKEN secret is blank"); return }

        try {
            val cleanUrl = if (dbUrl.endsWith("/")) dbUrl.dropLast(1) else dbUrl
            val body = JSONObject().apply {
                put("requests", JSONArray().put(JSONObject().apply {
                    put("type", "execute")
                    put("stmt", JSONObject().apply { put("sql", sql) })
                }))
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$cleanUrl/v2/pipeline")
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { 
                    callback(false, null, "OkHttp: ${e.message}") 
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val resBody = response.body?.string()
                        
                        // Diagnostic 2: Check HTTP Status (e.g., 401 Unauthorized, 404 Not Found)
                        if (!response.isSuccessful) {
                            callback(false, null, "HTTP ${response.code}: $resBody")
                            return
                        }
                        
                        if (resBody != null) {
                            val json = JSONObject(resBody)
                            val resultObj = json.getJSONArray("results").getJSONObject(0)
                            
                            // Diagnostic 3: Check Turso SQL Execution Errors (e.g., missing table)
                            if (resultObj.has("type") && resultObj.getString("type") == "error") {
                                val errObj = resultObj.getJSONObject("error").getString("message")
                                callback(false, null, "SQL Error: $errObj")
                                return
                            }
                            
                            val data = resultObj.getJSONObject("response").getJSONObject("result")
                            callback(true, data, null)
                        } else {
                            callback(false, null, "Empty Response")
                        }
                    } catch (e: Exception) {
                        callback(false, null, "JSON: ${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, null, "Builder: ${e.message}")
        }
    }
}
