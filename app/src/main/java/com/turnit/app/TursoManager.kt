package com.turnit.app

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TursoManager(private val dbUrl: String, private val authToken: String) {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun saveMessage(chatId: String, role: String, content: String) {
        val query = """
            INSERT INTO messages (chat_id, role, content, timestamp) 
            VALUES ('$chatId', '$role', '${content.replace("'", "''")}', CURRENT_TIMESTAMP);
        """.trimIndent()
        execute(query)
    }

    private fun execute(sql: String) {
        val body = JSONObject().apply {
            put("requests", JSONArray().put(JSONObject().apply {
                put("type", "execute")
                put("stmt", JSONObject().apply { put("sql", sql) })
            }))
        }.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$dbUrl/v2/pipeline")
            .addHeader("Authorization", "Bearer $authToken")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }
}
