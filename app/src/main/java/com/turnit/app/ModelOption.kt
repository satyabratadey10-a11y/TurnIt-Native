package com.turnit.app

data class ModelOption(
    val displayName: String,
    val modelId: String,
    val description: String,
    val apiType: Int,
    val shortLabel: String = "" // For Claude's UI icons
) {
    companion object {
        const val TYPE_GEMINI = 0
        const val TYPE_HUGGINGFACE = 1
    }
}
