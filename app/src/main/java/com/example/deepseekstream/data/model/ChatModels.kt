package com.example.deepseekstream.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionsRequest(
    val model: String,
    val stream: Boolean,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty") val presencePenalty: Double? = null,
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
    val temperature: Double,
)

@Serializable
data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean = true
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
