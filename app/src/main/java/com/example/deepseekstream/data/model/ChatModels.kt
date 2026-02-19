package com.example.deepseekstream.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionsRequest(
    val model: String,
    val stream: Boolean,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
