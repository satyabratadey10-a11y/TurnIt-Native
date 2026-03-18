package com.turnit.app
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

data class ChatResult(
    val text: String,
    val latencyMs: Long,
    val modelId: String,
    val routedDirect: Boolean
)

/**
 * 10 RPM token-bucket rate limiter.
 * maxRpm = Int.MAX_VALUE bypasses limiting (user API key mode).
 * delay() is suspending - never blocks the UI thread.
 */
class RateLimiter(private val maxRpm: Int) {
    private val windowMs = 60_000L
    private val ts = ArrayDeque<Long>(maxRpm.coerceAtMost(100))
    suspend fun acquire() {
        if (maxRpm == Int.MAX_VALUE) return
        val now = System.currentTimeMillis()
        while (ts.isNotEmpty() && now - ts.first() >= windowMs) ts.removeFirst()
        if (ts.size >= maxRpm) {
            delay(windowMs - (now - ts.first()) + 50)
        }
        ts.addLast(System.currentTimeMillis())
    }
}

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String,
    private val gatewayUrl: String = "https://gateway.turnit.ai/v1/chat"
) {
    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private var job: Job? = null
    private var userKey: String? = null
    private var handshake = ""
    // 10 RPM for gateway (Plan 1), unlimited for direct (user key)
    private val gwLimiter  = RateLimiter(10)
    private val dirLimiter = RateLimiter(Int.MAX_VALUE)

    fun setUserKey(k: String?) { userKey = k?.trim()?.ifEmpty { null } }
    fun setHandshake(h: String) { handshake = h }
    fun isActive() = job?.isActive == true
    fun cancel()   { job?.cancel(); job = null }

    fun send(
        prompt: String,
        model: ModelOption,
        onResult: (ChatResult) -> Unit,
        onError:  (String) -> Unit
    ) {
        job = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val direct = userKey != null
                    (if (direct) dirLimiter else gwLimiter).acquire()
                    val t0 = System.currentTimeMillis()
                    val text = if (direct)
                        directCall(prompt, model, userKey!!)
                    else
                        gatewayCall(prompt, model)
                    ChatResult(
                        text         = text,
                        latencyMs    = System.currentTimeMillis() - t0,
                        modelId      = model.modelId,
                        routedDirect = direct
                    )
                }
            }
            if (!isActive) return@launch
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onError(it.message ?: "Request failed") }
            )
        }
    }

    // ---- Gateway (TurnIt.ai tunnel, rate-managed) ----------------------
    private suspend fun gatewayCall(p: String, m: ModelOption): String {
        val body = JSONObject()
            .put("prompt",  p)
            .put("modelId", m.modelId)
            .put("apiType", m.apiType)
            .toString()
        val req = Request.Builder().url(gatewayUrl)
            .addHeader("X-TurnIt-Handshake", handshake)
            .addHeader("X-TurnIt-Client",    BuildConfig.VERSION_NAME)
            .post(body.toRequestBody(jt)).build()
        val raw = http.newCall(req).execute().use { it.body!!.string() }
        return JSONObject(raw).optString("text", raw)
    }

    // ---- Direct calls (user API key, bypass tunnel) --------------------
    private suspend fun directCall(
        p: String, m: ModelOption, key: String
    ): String = when (m.apiType) {
        ModelOption.TYPE_GEMINI      -> geminiCall(p, key)
        ModelOption.TYPE_HUGGINGFACE -> hfCall(p, m.modelId, key)
        else -> throw IllegalArgumentException("Unknown type: ${m.apiType}")
    }

    private suspend fun geminiCall(p: String, key: String): String {
        val body = JSONObject().put("contents",
            JSONArray().put(JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", p))))).toString()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-1.5-flash:generateContent?key=$key"
        val raw = http.newCall(Request.Builder().url(url)
            .post(body.toRequestBody(jt)).build())
            .execute().use { it.body!!.string() }
        return JSONObject(raw)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
            .getJSONObject(0).getString("text").trim()
    }

    private suspend fun hfCall(
        p: String, modelId: String, key: String
    ): String {
        val body = JSONObject().put("inputs", p)
            .put("parameters", JSONObject()
                .put("max_new_tokens", 512)
                .put("return_full_text", false)).toString()
        val raw = http.newCall(
            Request.Builder()
                .url("https://api-inference.huggingface.co/models/$modelId")
                .addHeader("Authorization", "Bearer $key")
                .post(body.toRequestBody(jt)).build())
            .execute().use { it.body!!.string() }
        return JSONArray(raw).getJSONObject(0)
            .getString("generated_text").trim()
    }

    fun close() = http.connectionPool.evictAll()
}
