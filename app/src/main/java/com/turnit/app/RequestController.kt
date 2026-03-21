package com.turnit.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Carrier for chat response data
data class ChatResult(
    val text: String,
    val latencyMs: Long,
    val modelId: String
)

// Suspending rate limiter to prevent API 429 errors
class RateLimiter(private val maxRpm: Int) {
    private val windowMs = 60_000L
    private val stamps = ArrayDeque<Long>()

    suspend fun acquire() {
        if (maxRpm == Int.MAX_VALUE) return
        val now = System.currentTimeMillis()
        while (stamps.isNotEmpty() && now - stamps.first() >= windowMs) {
            stamps.removeFirst()
        }
        if (stamps.size >= maxRpm) {
            val waitTime = windowMs - (now - stamps.first()) + 200L
            delay(waitTime)
        }
        stamps.addLast(System.currentTimeMillis())
    }
}

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String, // From BuildConfig
    private val hfKey: String      // From BuildConfig
) {
    companion object {
        // v1beta is required for the latest Flash/Pro models to avoid 404s
        const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        const val HF_BASE = "https://api-inference.huggingface.co/models"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null
    private var userGeminiKey: String? = null
    private var userHfKey: String? = null
    
    private val geminiLimiter = RateLimiter(10)
    private val hfLimiter = RateLimiter(10)
    private val noLimiter = RateLimiter(Int.MAX_VALUE)

    // Key Management
    fun setUserGeminiKey(k: String?) { userGeminiKey = k?.trim()?.ifEmpty { null } }
    fun setUserHfKey(k: String?) { userHfKey = k?.trim()?.ifEmpty { null } }
    fun isActive() = activeJob?.isActive == true
    fun cancel() { activeJob?.cancel(); activeJob = null }

    /**
     * Entry point for sending messages. Handles Dispatcher switching and Rate Limiting.
     */
    fun send(
        prompt: String,
        model: ModelOption,
        onResult: (ChatResult) -> Unit,
        onError: (String) -> Unit
    ) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val text = when (model.apiType) {
                        ModelOption.TYPE_GEMINI -> {
                            val key = userGeminiKey ?: geminiKey
                            val lim = if (userGeminiKey != null) noLimiter else geminiLimiter
                            lim.acquire()
                            callGemini(prompt, model.modelId, key)
                        }
                        ModelOption.TYPE_HUGGINGFACE -> {
                            val key = userHfKey ?: hfKey
                            val lim = if (userHfKey != null) noLimiter else hfLimiter
                            lim.acquire()
                            callHuggingFace(prompt, model.modelId, key)
                        }
                        else -> throw IllegalArgumentException("Unknown apiType: ${model.apiType}")
                    }
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            
            if (!isActive) return@launch
            
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { 
                    val errorMsg = it.message ?: "Request failed"
                    Log.e("TurnIt_API", "Error: $errorMsg")
                    onError(errorMsg) 
                }
            )
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        // Step 1: Sanitize modelId (ensure no "models/" prefix)
        val cleanId = modelId.removePrefix("models/")
        
        // Step 2: Build Payload
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            )).toString()

        // Step 3: Execute Request
        val url = "$GEMINI_BASE/$cleanId:generateContent?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .post(body.toRequestBody(jt))
            .build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val serverMsg = runCatching { 
                    JSONObject(raw).getJSONObject("error").getString("message") 
                }.getOrNull()
                throw RuntimeException("Gemini ${resp.code}: ${serverMsg ?: "Invalid Model or Key"}")
            }
            
            // Step 4: Parse Response
            JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text").trim()
        }
    }

    private suspend fun callHuggingFace(prompt: String, modelId: String, token: String): String {
        val body = JSONObject()
            .put("inputs", prompt)
            .put("parameters", JSONObject()
                .put("max_new_tokens", 512)
                .put("return_full_text", false)
                .put("temperature", 0.7)
            ).toString()

        val req = Request.Builder()
            .url("$HF_BASE/$modelId")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody(jt))
            .build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw RuntimeException("HF ${resp.code}: Model is likely loading. Try again in 30s.")
            }
            
            // HuggingFace returns an array: [{"generated_text": "..."}]
            val array = JSONArray(raw)
            if (array.length() == 0) throw RuntimeException("Empty response from HuggingFace")
            array.getJSONObject(0).getString("generated_text").trim()
        }
    }

    fun close() = http.connectionPool.evictAll()
}
