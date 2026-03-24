package com.turnit.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

// Unified Constants
const val MSG_USER = 0
const val MSG_AI = 1

@Composable
fun rememberRgbBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "rgb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase"
    )
    return Brush.linearGradient(
        colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Red),
        start = Offset(phase * 1000f, 0f),
        end = Offset(phase * 1000f + 500f, 500f),
        tileMode = TileMode.Mirror
    )
}

@Composable
fun TurnItLogo(modifier: Modifier = Modifier) {
    Text(
        text = "TurnIt",
        style = MaterialTheme.typography.displayMedium.copy(
            brush = rememberRgbBrush()
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages: List<Pair<String, Int>>,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit,
    onApiKey: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0B0E14)) {
                Spacer(Modifier.height(12.dp))
                TurnItLogo(Modifier.padding(16.dp))
                NavigationDrawerItem(label = { Text("New Chat") }, selected = false, onClick = { onNewChat(); scope.launch { drawerState.close() } })
                NavigationDrawerItem(label = { Text("API Key Settings") }, selected = false, onClick = { onApiKey(); scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { TurnItLogo() },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            bottomBar = {
                NeonInputBar(onSend)
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
                ChatList(messages)
            }
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(messages) { (text, type) ->
            Box(Modifier.fillMaxWidth(), contentAlignment = if (type == MSG_USER) Alignment.CenterEnd else Alignment.CenterStart) {
                Surface(
                    color = if (type == MSG_USER) Color(0x404285F4) else Color(0x401E1E1E),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.padding(8.dp).widthIn(max = 300.dp)
                ) {
                    Text(text = text, color = Color.White, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val brush = rememberRgbBrush()
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f).border(2.dp, brush, RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent, 
                focusedIndicatorColor = Color.Transparent, 
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Cyan)
        }
    }
}
