package com.turnit.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.turnit.app.ui.*
import com.turnit.app.models.ModelOption

class MainActivity : ComponentActivity() {
    private lateinit var reqCtrl: RequestController
    private lateinit var turso: TursoManager
    private val messages = mutableStateListOf<Pair<String, Int>>()
    
    private var userId by mutableStateOf<String?>(null)
    private var currentScreen by mutableStateOf("login")
    private var activeModel by mutableStateOf(QX_MODELS[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        turso = TursoManager(BuildConfig.TURSO_URL, BuildConfig.TURSO_TOKEN)
        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)

        setContent {
            TurnItTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (userId == null) {
                        if (currentScreen == "login") {
                            LoginScreen(
                                onLoginClick = { u, p -> 
                                    turso.login(u, p) { success, id -> 
                                        if(success) userId = id 
                                        else runOnUiThread { Toast.makeText(this@MainActivity, id, Toast.LENGTH_SHORT).show() }
                                    }
                                },
                                onSignupClick = { currentScreen = "signup" }
                            )
                        } else {
                            SignupScreen(
                                onSignupClick = { u, e, p -> 
                                    turso.signup(u, e, p) { success, id -> 
                                        if(success) userId = id 
                                        else runOnUiThread { Toast.makeText(this@MainActivity, id, Toast.LENGTH_SHORT).show() }
                                    }
                                },
                                onLoginClick = { currentScreen = "login" }
                            )
                        }
                    } else {
                        TurnItMainScreen(
                            messages = messages,
                            selectedModel = activeModel,
                            onModelChange = { activeModel = it },
                            onSend = { text -> sendMessage(text) },
                            onNewChat = { messages.clear() }
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
        messages.add("..." to MSG_AI)

        reqCtrl.send(text, activeModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
