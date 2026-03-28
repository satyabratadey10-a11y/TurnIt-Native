package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope

import com.turnit.app.ui.*
import com.turnit.app.ModelOption

class MainActivity : ComponentActivity() {
    private lateinit var reqCtrl: RequestController
    private val messages = mutableStateListOf<Pair<String, Int>>()
    
    // Auth State (Problem 2)
    private var isLoggedIn by mutableStateOf(false)
    private var currentScreen by mutableStateOf("login")

    // Global Model State (Problem 5 Fix)
    private var activeModel by mutableStateOf(
        ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Rapid", ModelOption.TYPE_GEMINI)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)

        setContent {
            TurnItTheme {
                when {
                    !isLoggedIn && currentScreen == "login" -> {
                        LoginScreen(
                            onLoginClick = { _, _ -> isLoggedIn = true },
                            onSignupClick = { currentScreen = "signup" }
                        )
                    }
                    !isLoggedIn && currentScreen == "signup" -> {
                        SignupScreen(
                            onSignupClick = { _, _, _ -> currentScreen = "login" },
                            onLoginClick = { currentScreen = "login" }
                        )
                    }
                    else -> {
                        TurnItMainScreen(
                            messages = messages,
                            initialModel = activeModel,
                            onModelChange = { activeModel = it }, // Model Switch Connection
                            onSend = { text -> sendMessage(text) },
                            onNewChat = { messages.clear() } // New Chat Fix
                        )
                    }
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(text to MSG_USER)
        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        // Pass the CURRENT active model to the AI controller
        reqCtrl.send(text, activeModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
