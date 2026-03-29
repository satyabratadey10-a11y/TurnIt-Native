package com.turnit.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turnit.app.models.ModelOption
import kotlinx.coroutines.launch

object QX {
    val VoidBlack = Color(0xFF0B0E14)
    val QuantumTeal = Color(0xFF008080)
    val GlassFill = Color(0x22FFFFFF)
    val GlassBorder = Color(0x44FFFFFF)
    val TextPrimary = Color(0xFFF0F4FF)
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
                LazyColumn(Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                    items(messages) { msg ->
                        val isUser = msg.second == MSG_USER
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = if(isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                            Surface(color = if(isUser) QX.QuantumTeal.copy(0.2f) else QX.GlassFill, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, QX.GlassBorder)) {
                                Text(msg.first, modifier = Modifier.padding(12.dp), color = QX.TextPrimary)
                            }
                        }
                    }
                }

                // FLOATING MODEL SELECTOR (RED BOX AREA)
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 90.dp), horizontalAlignment = Alignment.End) {
                    if (showModelMenu) {
                        Surface(Modifier.width(200.dp).padding(bottom = 8.dp), color = QX.VoidBlack, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, QX.GlassBorder)) {
                            Column {
                                QX_MODELS.forEach { model ->
                                    TextButton(onClick = { onModelChange(model); showModelMenu = false }) {
                                        Text(model.displayName, color = if(model == selectedModel) QX.QuantumTeal else QX.TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                    FloatingActionButton(onClick = { showModelMenu = !showModelMenu }, containerColor = QX.QuantumTeal, contentColor = Color.White, shape = CircleShape) {
                        Text(selectedModel.shortLabel)
                    }
                }

                // Input Bar
                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), color = QX.GlassFill, shape = CircleShape, border = BorderStroke(1.dp, QX.QuantumTeal)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        var text by remember { mutableStateOf("") }
                        TextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                        IconButton(onClick = { onSend(text); text = "" }) { Icon(Icons.Default.Send, null, tint = QX.QuantumTeal) }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit, onSignupClick: () -> Unit) {
    Column(Modifier.fillMaxSize().background(QX.VoidBlack), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button(onClick = { onLoginClick("admin", "pass") }) { Text("LOGIN TO TURNIT") }
        TextButton(onClick = onSignupClick) { Text("Create Account", color = QX.QuantumTeal) }
    }
}

@Composable
fun SignupScreen(onSignupClick: (String, String, String) -> Unit, onLoginClick: () -> Unit) {
    Column(Modifier.fillMaxSize().background(QX.VoidBlack), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button(onClick = { onSignupClick("user", "email", "pass") }) { Text("SIGN UP") }
        TextButton(onClick = onLoginClick) { Text("Back to Login", color = QX.QuantumTeal) }
    }
}
