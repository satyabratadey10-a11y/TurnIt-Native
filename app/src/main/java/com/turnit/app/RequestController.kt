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
        // v1beta is required for gemini-1.5-flash to avoid 404 errors
        const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        const val HF_BASE = "https://api-inference.huggingface.co/models"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null
    private var userGeminiKey: String? = null

    fun setUserGeminiKey(k: String?) { userGeminiKey = k?.trim()?.ifEmpty { null } }
    fun close() = http.connectionPool.evictAll()

    fun send(prompt: String, model: ModelOption, onResult: (ChatResult) -> Unit, onError: (String) -> Unit) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val text = when (model.apiType) {
                        ModelOption.TYPE_GEMINI -> callGemini(prompt, model.modelId, userGeminiKey ?: geminiKey)
                        ModelOption.TYPE_HUGGINGFACE -> callHuggingFace(prompt, model.modelId, hfKey)
                        else -> "Unknown API"
                    }
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            result.fold(onSuccess = { onResult(it) }, onFailure = { onError(it.message ?: "Error") })
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        // Remove 'models/' prefix if it exists to prevent double-pathing
        val cleanId = modelId.removePrefix("models/")
        val body = JSONObject().put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )).toString()

        val url = "$GEMINI_BASE/$cleanId:generateContent?key=$apiKey"
        val req = Request.Builder().url(url).post(body.toRequestBody(jt)).build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Gemini Error: ${resp.code}")
            JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    private suspend fun callHuggingFace(prompt: String, modelId: String, token: String): String {
        val body = JSONObject().put("inputs", prompt).toString()
        val req = Request.Builder().url("$HF_BASE/$modelId")
            .addHeader("Authorization", "Bearer $token").post(body.toRequestBody(jt)).build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (resp.code == 503 || resp.code == 410) throw RuntimeException("Model is loading... Try in 30s")
            if (!resp.isSuccessful) throw RuntimeException("HF Error: ${resp.code}")
            JSONArray(raw).getJSONObject(0).getString("generated_text")
        }
    }
}
