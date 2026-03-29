package com.turnit.app.ui
import com.turnit.app.models.ModelOption

val QX_MODELS = listOf(
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - Rapid", ModelOption.TYPE_GEMINI, "G3F"),
    ModelOption("Gemini 2.5 Fast", "gemini-2.5-fast", "Google - Balanced", ModelOption.TYPE_GEMINI, "G2F"),
    ModelOption("Qwen 3.5 Novita", "qwen-3.5-novita", "Alibaba - Logic", ModelOption.TYPE_HUGGINGFACE, "Q35"),
    ModelOption("DeepSeek V3", "deepseek-v3", "Code Specialist", ModelOption.TYPE_HUGGINGFACE, "DSV")
)

const val MSG_USER = 0
const val MSG_AI   = 1
