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

data class ChatResult(val text: String, val latencyMs: Long, val modelId: String)

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    companion object {
        const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1/models"
        const val HF_BASE = "https://api-inference.huggingface.co/models"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null

    fun send(prompt: String, model: ModelOption, onResult: (ChatResult) -> Unit, onError: (String) -> Unit) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    
                    // CRITICAL FIX: Explicit check of the apiType
                    val text = when (model.apiType) {
                        ModelOption.TYPE_GEMINI -> {
                            callGemini(prompt, model.modelId, geminiKey)
                        }
                        ModelOption.TYPE_HUGGINGFACE -> {
                            callHuggingFace(prompt, model.modelId, hfKey)
                        }
                        else -> throw IllegalArgumentException("Unsupported API Type: ${model.apiType}")
                    }
                    
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(it) },
                    onFailure = { 
                        // If it fails here, we know EXACTLY which API was hit
                        val apiName = if (model.apiType == ModelOption.TYPE_GEMINI) "Gemini" else "HuggingFace"
                        onError("$apiName Error: ${it.message}") 
                    }
                )
            }
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        val cleanId = modelId.removePrefix("models/")
        val body = JSONObject().put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )).toString()

        val url = "$GEMINI_BASE/$cleanId:generateContent?key=$apiKey"
        val req = Request.Builder().url(url).post(body.toRequestBody(jt)).build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    private suspend fun callHuggingFace(prompt: String, modelId: String, token: String): String {
        val body = JSONObject().put("inputs", prompt).toString()
        val req = Request.Builder().url("$HF_BASE/$modelId")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody(jt)).build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (resp.code == 503) throw RuntimeException("Model is still loading on HF")
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            
            // HuggingFace returns an array: [{"generated_text": "..."}]
            val jsonArray = JSONArray(raw)
            jsonArray.getJSONObject(0).getString("generated_text")
        }
    }

    fun close() = http.connectionPool.evictAll()
}
