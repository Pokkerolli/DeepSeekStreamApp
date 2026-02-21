package com.example.deepseekstream.domain.model

data class TokenUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)

data class CompletionMetrics(
    val provider: LlmProvider,
    val model: String,
    val latencyMs: Long,
    val usage: TokenUsage,
    val estimatedCostUsd: Double,
    val pricingSourceUrl: String
)

sealed interface LLMStreamEvent {
    data class Chunk(val text: String) : LLMStreamEvent
    data class Completed(val metrics: CompletionMetrics) : LLMStreamEvent
}

