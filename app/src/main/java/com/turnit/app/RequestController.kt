package com.turnit.app

import com.turnit.app.models.ModelOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    fun send(prompt: String, model: ModelOption, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (model.apiType == ModelOption.TYPE_GEMINI) {
                        callGemini(prompt, geminiKey, model.modelId)
                    } else {
                        callHuggingFace(prompt, hfKey, model.modelId)
                    }
                }
                onSuccess(result)
            } catch (e: Exception) {
                onError(e.message ?: "Connection Error")
            }
        }
    }

    private fun callGemini(prompt: String, key: String, modelId: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$key")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        
        val body = """{"contents":[{"parts":[{"text":"$prompt"}]}]}"""
        conn.outputStream.write(body.toByteArray())
        
        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun callHuggingFace(prompt: String, key: String, modelId: String): String {
        // Implementation for Qwen 3.5 via Novita/HF
        return "Qwen 3.5 logic coming in next build..."
    }
}
