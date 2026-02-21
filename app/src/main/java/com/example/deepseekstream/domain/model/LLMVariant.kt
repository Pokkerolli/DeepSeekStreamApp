package com.example.deepseekstream.domain.model

enum class LlmProvider {
    DEEPSEEK,
    OPEN_ROUTER
}

data class LLMVariant(
    val outputKey: String,
    val provider: LlmProvider = LlmProvider.DEEPSEEK,
    val model: String? = null,
    val systemPrompt: String?,
    val maxTokens: Int,
    val topP: Double = 1.0,
    val frequencyPenalty: Double = 0.0,
    val presencePenalty: Double = 0.0,
    val temperature: Double = 1.0,
)
