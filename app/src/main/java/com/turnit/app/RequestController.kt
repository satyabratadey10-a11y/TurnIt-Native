package com.turnit.app

import com.turnit.app.models.ModelOption
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun send(prompt: String, model: ModelOption, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                if (model.apiType == ModelOption.TYPE_GEMINI) {
                    callGemini(prompt, model.modelId, onSuccess, onError)
                } else {
                    callHuggingFace(prompt, model.modelId, onSuccess, onError)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
            }
        }
    }

    private fun callGemini(prompt: String, modelId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$geminiKey"
        val body = """{"contents":[{"parts":[{"text":"${prompt.replace("\"", "\\\"")}"}]}]}""".toRequestBody(JSON)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "Unknown Gemini Error"
                scope.launch(Dispatchers.Main) { onError(err) }
                return
            }
            val resStr = response.body?.string()
            if (resStr != null) {
                val json = JSONObject(resStr)
                val text = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                scope.launch(Dispatchers.Main) { onSuccess(text) }
            }
        }
    }

    private fun callHuggingFace(prompt: String, modelId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val url = "https://api.novita.ai/v3/openai/chat/completions"
        val jsonBody = JSONObject().apply {
            put("model", modelId)
            put("messages", org.json.JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }
        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $hfKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                scope.launch(Dispatchers.Main) { onError("Novita Error: ${response.code}") }
                return
            }
            val resStr = response.body?.string()
            if (resStr != null) {
                val json = JSONObject(resStr)
                val text = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                scope.launch(Dispatchers.Main) { onSuccess(text) }
            }
        }
    }
}
