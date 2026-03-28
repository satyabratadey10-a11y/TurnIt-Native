package com.turnit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.turnit.app.models.ModelOption

const val MSG_USER = 0
const val MSG_AI   = 1

object QX {
    val VoidBlack      = Color(0xFF0B0E14)
    val QuantumTeal    = Color(0xFF008080)
    val GlassFill      = Color(0x1AFFFFFF)
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
    initialModel: ModelOption,
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
                Text("TurnIt History", modifier = Modifier.padding(16.dp), color = QX.QuantumTeal, fontWeight = FontWeight.Bold)
                NavigationDrawerItem(
                    label = { Text("New Chat") },
                    selected = false,
                    onClick = { onNewChat(); scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = QX.VoidBlack,
            topBar = {
                TopAppBar(
                    title = { Text("TurnIt", color = QX.QuantumTeal) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = QX.QuantumTeal)
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { showModelMenu = true }) {
                                Text(initialModel.shortLabel, color = QX.QuantumTeal)
                            }
                            DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                                QX_MODELS.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.displayName) },
                                        onClick = { onModelChange(model); showModelMenu = false }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QX.VoidBlack)
                )
            },
            bottomBar = { NeonInputBar(onSend = onSend) }
        ) { pad ->
            ChatList(messages, Modifier.padding(pad))
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>, modifier: Modifier) {
    val state = rememberLazyListState()
    LazyColumn(state = state, modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(messages) { msg ->
            val align = if(msg.second == MSG_USER) Alignment.CenterEnd else Alignment.CenterStart
            Box(Modifier.fillMaxWidth(), contentAlignment = align) {
                Text(
                    text = msg.first,
                    modifier = Modifier.padding(vertical = 4.dp).background(QX.GlassFill, RoundedCornerShape(12.dp)).padding(12.dp),
                    color = QX.TextPrimary
                )
            }
        }
    }
}

@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(16.dp)) {
        TextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.weight(1f).background(QX.GlassFill, CircleShape),
            placeholder = { Text("Message...") },
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend(text); text = "" })
        )
        IconButton(onClick = { onSend(text); text = "" }) { Icon(Icons.Default.Send, null, tint = QX.QuantumTeal) }
    }
}

val QX_MODELS = listOf(
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - Rapid", ModelOption.TYPE_GEMINI, "G3F"),
    ModelOption("Qwen 3.5 Novita", "qwen-3.5-novita", "Alibaba - Logic", ModelOption.TYPE_HUGGINGFACE, "Q3N")
)
