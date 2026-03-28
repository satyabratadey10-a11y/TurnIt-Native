package com.turnit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turnit.app.models.ModelOption
import kotlinx.coroutines.launch

const val MSG_USER = 0
const val MSG_AI   = 1

object QX {
    val VoidBlack      = Color(0xFF0B0E14)
    val QuantumTeal    = Color(0xFF008080)
    val GlassFill      = Color(0x22FFFFFF)
    val GlassBorder    = Color(0x33FFFFFF)
    val TextPrimary    = Color(0xFFF0F4FF)
}

@Composable
fun TurnItTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = QX.VoidBlack), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages: List<Pair<String, Int>>,
    selectedModel: ModelOption,
    onModelChange: (ModelOption) -> Unit,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showModelMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = QX.VoidBlack) {
                Text("TurnIt History", modifier = Modifier.padding(16.dp), color = QX.QuantumTeal)
                NavigationDrawerItem(label = { Text("New Chat") }, selected = false, onClick = { onNewChat(); scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            containerColor = QX.VoidBlack,
            topBar = {
                TopAppBar(
                    title = { Text("TurnIt", color = QX.QuantumTeal) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = QX.QuantumTeal) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QX.VoidBlack)
                )
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                ChatList(messages)

                // MODEL SELECTOR: Floating in your RED BOX area
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp), horizontalAlignment = Alignment.End) {
                    if (showModelMenu) {
                        Card(colors = CardDefaults.cardColors(containerColor = QX.VoidBlack.copy(0.9f)), border = androidx.compose.foundation.BorderStroke(1.dp, QX.GlassBorder)) {
                            Column {
                                QX_MODELS.forEach { model ->
                                    TextButton(onClick = { onModelChange(model); showModelMenu = false }) {
                                        Text(model.displayName, color = if(model == selectedModel) QX.QuantumTeal else QX.TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { showModelMenu = !showModelMenu },
                        colors = ButtonDefaults.buttonColors(containerColor = QX.QuantumTeal.copy(0.2f)),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QX.QuantumTeal)
                    ) {
                        Text(selectedModel.shortLabel, color = QX.QuantumTeal, fontSize = 10.sp)
                    }
                }

                Box(Modifier.align(Alignment.BottomCenter)) {
                    NeonInputBar(onSend = onSend)
                }
            }
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>) {
    LazyColumn(Modifier.fillMaxSize().padding(bottom = 80.dp)) {
        items(messages) { msg ->
            val isUser = msg.second == MSG_USER
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = if(isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                Surface(
                    color = if(isUser) QX.QuantumTeal.copy(0.2f) else QX.GlassFill,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, QX.GlassBorder)
                ) {
                    Text(msg.first, modifier = Modifier.padding(12.dp), color = QX.TextPrimary)
                }
            }
        }
    }
}

@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(16.dp).background(QX.GlassFill, CircleShape).border(1.dp, QX.QuantumTeal, CircleShape).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message...") },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
        IconButton(onClick = { onSend(text); text = "" }) { Icon(Icons.Default.Send, null, tint = QX.QuantumTeal) }
    }
}

val QX_MODELS = listOf(
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Fast", ModelOption.TYPE_GEMINI, "G3F"),
    ModelOption("Qwen 3.5 Novita", "qwen-3.5-novita", "Logic", ModelOption.TYPE_HUGGINGFACE, "Q3N")
)
