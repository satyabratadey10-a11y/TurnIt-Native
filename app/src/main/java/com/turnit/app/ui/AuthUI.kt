package com.turnit.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// =========================================================
// AUTH COLOUR PALETTE (mirrors QX object in Composables)
// =========================================================

private object AuthQX {
    val VoidBlack     = Color(0xFF0B0E14)
    val NebulaSurface = Color(0xFF1A1225)
    val QuantumTeal   = Color(0xFF008080)
    val TealGlow      = Color(0xFF00B8B8)
    val PurpleGlow    = Color(0xFF7B4FBF)
    val AuroraBlue    = Color(0xFF00D1FF)
    val NeonRed       = Color(0xFFF87171)
    val NeonGreen     = Color(0xFF4ADE80)
    val GlassFill     = Color(0x1AFFFFFF)
    val GlassBorder   = Color(0x33FFFFFF)
    val FieldFill     = Color(0x14FFFFFF)
    val TextPrimary   = Color(0xFFF0F4FF)
    val TextMuted     = Color(0xFF8A9BB5)
    val ErrorRed      = Color(0xFFF87171)
}

// =========================================================
// SHARED AUTH TYPOGRAPHY
// =========================================================

private val authDisplay = TextStyle(
    fontSize      = 28.sp,
    fontWeight    = FontWeight.Bold,
    letterSpacing = 0.06.sp
)

private val authBody = TextStyle(
    fontSize      = 14.sp,
    letterSpacing = 0.01.sp
)

private val authLabel = TextStyle(
    fontSize      = 11.sp,
    letterSpacing = 0.08.sp,
    fontWeight    = FontWeight.Medium
)

// =========================================================
// RGB FLOW HELPERS (self-contained, no Composables import)
// =========================================================

private fun authLerpColor(a: Color, b: Color, t: Float) = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = 1f
)

private fun authLerpRgb(t: Float): Color {
    val stops = listOf(
        AuthQX.NeonRed, AuthQX.NeonGreen,
        AuthQX.AuroraBlue, AuthQX.PurpleGlow,
        AuthQX.NeonRed
    )
    val scaled = t.coerceIn(0f, 1f) * (stops.size - 1)
    val idx    = scaled.toInt().coerceIn(0, stops.size - 2)
    return authLerpColor(stops[idx], stops[idx + 1], scaled - idx)
}

// =========================================================
// QUANTUM ENTRY BUTTON
// Rotating RGB border + teal-to-purple fill.
// drawWithCache: CornerRadius + Stroke allocated once per size.
// =========================================================

@Composable
private fun QuantumEntryButton(
    label:    String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    enabled:  Boolean  = true
) {
    val cornerRadius = 14.dp
    val shape        = RoundedCornerShape(cornerRadius)

    val inf = rememberInfiniteTransition(label = "qe_border")
    val deg by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "qe_deg"
    )
    val borderColors = listOf(
        AuthQX.NeonRed, AuthQX.QuantumTeal,
        AuthQX.NeonGreen, AuthQX.AuroraBlue,
        AuthQX.PurpleGlow, AuthQX.NeonRed
    )

    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = shape,
        colors   = ButtonDefaults.buttonColors(
            containerColor     = Color.Transparent,
            contentColor       = AuthQX.TextPrimary,
            disabledContainerColor = AuthQX.GlassFill,
            disabledContentColor   = AuthQX.TextMuted
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(AuthQX.QuantumTeal, AuthQX.PurpleGlow)
                ),
                shape
            )
            .drawWithCache {
                val sw  = 2.dp.toPx()
                val cr  = CornerRadius(cornerRadius.toPx())
                val stk = Stroke(sw)
                onDrawWithContent {
                    drawContent()
                    val rad = Math.toRadians(deg.toDouble())
                    val r   = maxOf(size.width, size.height)
                    val sx  = (size.width  / 2 + r * cos(rad)).toFloat()
                    val sy  = (size.height / 2 + r * sin(rad)).toFloat()
                    drawRoundRect(
                        brush        = Brush.sweepGradient(
                            borderColors, Offset(sx, sy)),
                        size         = size,
                        cornerRadius = cr,
                        style        = stk
                    )
                }
            }
    ) {
        Text(
            text  = label,
            style = authBody.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.12.sp,
                color         = AuthQX.TextPrimary
            )
        )
    }
}

// =========================================================
// NEON FIELD
// Glass fill + teal focus glow + glass border.
// No BlurMaskFilter (Y51a safe).
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeonField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    modifier:      Modifier          = Modifier,
    keyboardType:  KeyboardType      = KeyboardType.Text,
    isPassword:    Boolean           = false
) {
    val isFocused  = remember { mutableStateOf(false) }
    val borderColor = if (isFocused.value) AuthQX.QuantumTeal
                      else AuthQX.GlassBorder
    val fieldShape = RoundedCornerShape(12.dp)

    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(
                placeholder,
                style = authBody.copy(
                    color = AuthQX.TextMuted.copy(alpha = 0.5f)
                )
            )
        },
        singleLine            = true,
        textStyle             = authBody.copy(color = AuthQX.TextPrimary),
        visualTransformation  = if (isPassword)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions       = KeyboardOptions(
            keyboardType = keyboardType
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor     = AuthQX.FieldFill,
            unfocusedContainerColor   = AuthQX.FieldFill,
            focusedIndicatorColor     = Color.Transparent,
            unfocusedIndicatorColor   = Color.Transparent,
            disabledIndicatorColor    = Color.Transparent,
            cursorColor               = AuthQX.QuantumTeal,
            focusedTextColor          = AuthQX.TextPrimary,
            unfocusedTextColor        = AuthQX.TextPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(fieldShape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = fieldShape
            )
    )
}

// =========================================================
// AUTH CARD SHELL
// Shared glassmorphic card wrapping login / signup.
// =========================================================

@Composable
private fun AuthCard(
    modifier: Modifier = Modifier,
    content:  @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        AuthQX.NebulaSurface.copy(alpha = 0.85f),
                        AuthQX.VoidBlack.copy(alpha = 0.95f)
                    )
                ),
                cardShape
            )
            .border(
                width = 1.dp,
                color = AuthQX.GlassBorder,
                shape = cardShape
            )
            .padding(horizontal = 28.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// =========================================================
// NEBULA AUTH BACKGROUND
// Three-layer gradient. No RenderEffect (API 30 compat).
// =========================================================

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthQX.VoidBlack)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AuthQX.PurpleGlow.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AuthQX.QuantumTeal.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(0f, Float.POSITIVE_INFINITY),
                    radius = 700f
                )
            )
    )
}

// =========================================================
// LOGIN SCREEN
// =========================================================

@Composable
fun LoginScreen(
    onLoginClick:    (username: String, password: String) -> Unit,
    onSignupClick:   () -> Unit,
    errorMessage:    String? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                "TurnIt",
                style = authDisplay.copy(
                    brush = Brush.linearGradient(
                        listOf(
                            AuthQX.QuantumTeal,
                            AuthQX.AuroraBlue,
                            AuthQX.PurpleGlow
                        )
                    )
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "QUANTUM INTERFACE",
                style = authLabel.copy(color = AuthQX.TextMuted)
            )
            Spacer(Modifier.height(36.dp))

            AuthCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "ACCESS NODE",
                        style = authLabel.copy(color = AuthQX.QuantumTeal)
                    )
                    Spacer(Modifier.height(4.dp))
                    NeonField(
                        value         = username,
                        onValueChange = { username = it },
                        placeholder   = "username"
                    )
                    NeonField(
                        value         = password,
                        onValueChange = { password = it },
                        placeholder   = "password",
                        keyboardType  = KeyboardType.Password,
                        isPassword    = true
                    )
                    if (errorMessage != null) {
                        Text(
                            errorMessage,
                            style = authLabel.copy(color = AuthQX.ErrorRed)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    QuantumEntryButton(
                        label   = "QUANTUM ENTRY",
                        onClick = {
                            onLoginClick(username.trim(), password)
                        },
                        enabled = username.isNotBlank() &&
                                  password.isNotBlank()
                    )
                    TextButton(onClick = onSignupClick) {
                        Text(
                            "New user? Initialize account",
                            style = authBody.copy(
                                color = AuthQX.AuroraBlue
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================
// SIGNUP SCREEN
// =========================================================

@Composable
fun SignupScreen(
    onSignupClick:  (username: String, email: String, password: String) -> Unit,
    onLoginClick:   () -> Unit,
    errorMessage:   String? = null
) {
    var username by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }

    val passwordsMatch = password == confirm || confirm.isEmpty()
    val allFilled      = username.isNotBlank() && email.isNotBlank() &&
                         password.isNotBlank() && confirm.isNotBlank() &&
                         passwordsMatch

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "TurnIt",
                style = authDisplay.copy(
                    brush = Brush.linearGradient(
                        listOf(
                            AuthQX.NeonGreen,
                            AuthQX.AuroraBlue,
                            AuthQX.PurpleGlow
                        )
                    )
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "INITIALIZE ACCOUNT",
                style = authLabel.copy(color = AuthQX.TextMuted)
            )
            Spacer(Modifier.height(28.dp))

            AuthCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "NEW OPERATOR",
                        style = authLabel.copy(color = AuthQX.QuantumTeal)
                    )
                    Spacer(Modifier.height(2.dp))
                    NeonField(
                        value         = username,
                        onValueChange = { username = it },
                        placeholder   = "username"
                    )
                    NeonField(
                        value         = email,
                        onValueChange = { email = it },
                        placeholder   = "email address",
                        keyboardType  = KeyboardType.Email
                    )
                    NeonField(
                        value         = password,
                        onValueChange = { password = it },
                        placeholder   = "password",
                        keyboardType  = KeyboardType.Password,
                        isPassword    = true
                    )
                    NeonField(
                        value         = confirm,
                        onValueChange = { confirm = it },
                        placeholder   = "confirm password",
                        keyboardType  = KeyboardType.Password,
                        isPassword    = true
                    )
                    if (!passwordsMatch) {
                        Text(
                            "Passwords do not match",
                            style = authLabel.copy(color = AuthQX.ErrorRed)
                        )
                    }
                    if (errorMessage != null) {
                        Text(
                            errorMessage,
                            style = authLabel.copy(color = AuthQX.ErrorRed)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    QuantumEntryButton(
                        label   = "ACTIVATE OPERATOR",
                        onClick = {
                            onSignupClick(
                                username.trim(),
                                email.trim(),
                                password
                            )
                        },
                        enabled = allFilled
                    )
                    TextButton(onClick = onLoginClick) {
                        Text(
                            "Existing operator? Access node",
                            style = authBody.copy(
                                color = AuthQX.AuroraBlue
                            )
                        )
                    }
                }
            }
        }
    }
}
