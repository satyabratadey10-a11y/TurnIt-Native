package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.turnit.app.ui.*
import com.turnit.app.models.ModelOption

class MainActivity : ComponentActivity() {
    private lateinit var reqCtrl: RequestController
    private val messages = mutableStateListOf<Pair<String, Int>>()
    private var isLoggedIn by mutableStateOf(false)
    private var activeModel by mutableStateOf(QX_MODELS[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reqCtrl = RequestController(lifecycleScope, "", "")

        setContent {
            TurnItTheme {
                if (!isLoggedIn) {
                    // Temporarily using simple login trigger until Claude Auth is added
                    Button(onClick = { isLoggedIn = true }, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        Text("LOGIN TO TURNIT")
                    }
                } else {
                    TurnItMainScreen(
                        messages = messages,
                        initialModel = activeModel,
                        onModelChange = { activeModel = it },
                        onSend = { text -> sendMessage(text) },
                        onNewChat = { messages.clear() }
                    )
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(text to MSG_USER)
        reqCtrl.send(text, activeModel, null, { messages.add(it to MSG_AI) }, { })
    }
}
