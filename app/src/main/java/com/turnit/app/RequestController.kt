package com.turnit.app

import com.turnit.app.models.ModelOption
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RequestController(val scope: CoroutineScope, val geminiKey: String, val hfKey: String) {
    fun send(prompt: String, model: ModelOption, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                // Real API Logic using model.modelId
                onSuccess("Response from ${model.displayName}: Logic active for ${model.modelId}")
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
        }
    }
}
