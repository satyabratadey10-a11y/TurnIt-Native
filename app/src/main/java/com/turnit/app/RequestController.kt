package com.turnit.app

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.net.SocketTimeoutException

data class ChatResult(val text: String, val latencyMs: Long, val modelId: String)

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val TAG = "RequestController"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var activeJob: Job? = null

    fun send(prompt: String, model: ModelOption, onResult: (ChatResult) -> Unit, onError: (String) -> Unit) {
        activeJob?.cancel() // Cancel previous request if any
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    
                    // Validate API keys
                    if (geminiKey.isBlank()) {
                        throw RuntimeException("GEMINI_API_KEY not configured. Check BuildConfig.")
                    }
                    
                    val text = callGemini(prompt, model.modelId, geminiKey)
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { e ->
                    Log.e(TAG, "Error in request", e)
                    val errorMsg = when (e) {
                        is SocketTimeoutException -> "Connection timeout. Check your internet."
                        is java.net.UnknownHostException -> "Network error. Check internet connection."
                        is RuntimeException -> e.message ?: "API Error"
                        else -> "Error: ${e.message ?: "Unknown error"}"
                    }
                    onError(errorMsg)
                }
            )
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        // Validate inputs
        if (prompt.isBlank()) throw RuntimeException("Prompt cannot be empty")
        if (apiKey.isBlank()) throw RuntimeException("API key is missing or empty")
        
        val cleanId = modelId.removePrefix("models/")
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }.toString()

        val url = "$BASE_URL/$cleanId:generateContent?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jt))
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string() ?: ""
                
                if (!resp.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(raw)
                            .getJSONObject("error")
                            .getString("message")
                    } catch (e: Exception) {
                        "HTTP ${resp.code}: ${resp.message}"
                    }
                    throw RuntimeException("Gemini API Error: $errorMsg")
                }
                
                // Parse response safely
                val jsonResponse = JSONObject(raw)
                val candidates = jsonResponse.optJSONArray("candidates")
                    ?: throw RuntimeException("No candidates in response")
                
                if (candidates.length() == 0) {
                    throw RuntimeException("Empty candidates array")
                }
                
                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                
                if (content.isBlank()) {
                    throw RuntimeException("Empty response text from API")
                }
                
                content
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Call Failed: ${e.message}", e)
            throw e
        }
    }

    fun close() = http.connectionPool.evictAll()
}
