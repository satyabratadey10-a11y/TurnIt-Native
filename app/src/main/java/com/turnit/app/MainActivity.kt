package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.lifecycleScope

import com.turnit.app.ui.MSG_USER
import com.turnit.app.ui.MSG_AI
import com.turnit.app.ui.TurnItMainScreen
import com.turnit.app.ui.TurnItTheme

class MainActivity : ComponentActivity() {

    private lateinit var reqCtrl: RequestController
    private val messages = mutableStateListOf<Pair<String, Int>>()
    
    // Create the ModelOption object for Gemini 3 Flash
    private val defaultModel = ModelOption(
        name = "Gemini 3 Flash",
        id = "gemini-3-flash-preview",
        description = "Google - Rapid",
        type = ModelOption.TYPE_GEMINI
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reqCtrl = RequestController(
            scope = lifecycleScope, 
            geminiKey = BuildConfig.GEMINI_API_KEY, 
            hfKey = BuildConfig.HUGGINGFACE_API_KEY
        )

        setContent {
            TurnItTheme {
                TurnItMainScreen(
                    messages = messages,
                    onSend = { text -> sendMessage(text) },
                    onNewChat = { messages.clear() }
                )
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        messages.add(text to MSG_USER)
        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        // FIX: Passing the defaultModel object instead of a String
        reqCtrl.send(text, defaultModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
