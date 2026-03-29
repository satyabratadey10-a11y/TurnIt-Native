package com.turnit.app

import com.turnit.app.models.ModelOption
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
                    callGemini(prompt, model.modelId, history, onSuccess, onError)
                } else {
                    callHuggingFace(prompt, model.modelId, history, onSuccess, onError)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
            }
        }
    }

    private fun callGemini(prompt: String, modelId: String, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$geminiKey"
        
        val contentsArray = JSONArray()
        history?.forEach { msg ->
            contentsArray.put(JSONObject().apply {
                put("role", msg.first)
                put("parts", JSONArray().put(JSONObject().apply { put("text", msg.second) }))
            })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
        })

        val body = JSONObject().put("contents", contentsArray).toString().toRequestBody(JSON)
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

    private fun callHuggingFace(prompt: String, modelId: String, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val url = "https://router.huggingface.co/v1/chat/completions"
        
        val messagesArray = JSONArray()
        history?.forEach { msg ->
            val role = if (msg.first == "model") "assistant" else "user"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", msg.second)
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val jsonBody = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArray)
            put("stream", false)
        }
        
        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $hfKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "Unknown Router Error"
                scope.launch(Dispatchers.Main) { onError("HF Error ${response.code}: $err") }
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
