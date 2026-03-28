package com.turnit.app
import com.turnit.app.models.ModelOption
import kotlinx.coroutines.*

class RequestController(val scope: CoroutineScope, val geminiKey: String, val hfKey: String) {
    fun send(prompt: String, model: ModelOption, history: List<Pair<String, String>>?, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch { onSuccess("AI Response using ${model.displayName}") }
    }
}
