package com.example.deepseekstream.domain.model

data class DeepSeekVariant(
    val outputKey: String,
    val systemPrompt: String?,
    val maxTokens: Int,
    val temperature: Double = 1.0,
)
