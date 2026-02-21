package com.example.deepseekstream.data.repository

import com.example.deepseekstream.BuildConfig
import com.example.deepseekstream.data.model.ChatCompletionsRequest
import com.example.deepseekstream.data.model.ChatMessage
import com.example.deepseekstream.data.network.DeepSeekApi
import com.example.deepseekstream.data.network.OpenRouterApi
import com.example.deepseekstream.data.parser.StreamingParser
import com.example.deepseekstream.domain.model.CompletionMetrics
import com.example.deepseekstream.domain.model.LLMStreamEvent
import com.example.deepseekstream.domain.model.LLMVariant
import com.example.deepseekstream.domain.model.LlmProvider
import com.example.deepseekstream.domain.model.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.roundToInt

class LLMRepository(
    private val deepseekApi: DeepSeekApi,
    private val openRouter: OpenRouterApi,
    private val parser: StreamingParser
) {
    private data class Pricing(
        val inputPer1MUsd: Double,
        val outputPer1MUsd: Double,
        val sourceUrl: String
    )

    fun streamCompletion(question: String, variant: LLMVariant): Flow<String> = flow {
        streamCompletionWithMetrics(question, variant).collect { event ->
            when (event) {
                is LLMStreamEvent.Chunk -> emit(event.text)
                is LLMStreamEvent.Completed -> Unit
            }
        }
    }

    fun streamCompletionWithMetrics(question: String, variant: LLMVariant): Flow<LLMStreamEvent> {
        return when (variant.provider) {
            LlmProvider.DEEPSEEK -> streamFromDeepSeek(question, variant)
            LlmProvider.OPEN_ROUTER -> streamFromOpenRouter(question, variant)
        }
    }

    private fun streamFromDeepSeek(question: String, variant: LLMVariant): Flow<LLMStreamEvent> = flow {
        if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) {
            throw IllegalStateException("DEEPSEEK_API_KEY is empty. Add it to local.properties")
        }

        val messages = buildMessages(question, variant)
        val request = ChatCompletionsRequest(
            model = variant.model ?: BuildConfig.DEEPSEEK_MODEL,
            stream = true,
            messages = messages,
            maxTokens = variant.maxTokens,
            topP = variant.topP,
            frequencyPenalty = variant.frequencyPenalty,
            presencePenalty = variant.presencePenalty,
            temperature = variant.temperature,
        )

        val startedAt = System.currentTimeMillis()
        val response = deepseekApi.chatCompletionsStream(request)

        if (!response.isSuccessful) {
            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            throw IllegalStateException("HTTP $code: ${errorBody.ifBlank { "Request failed" }}")
        }

        val body = response.body() ?: throw IllegalStateException("Empty response body")
        val generated = StringBuilder()
        var usage: TokenUsage? = null

        body.use { responseBody ->
            val source = responseBody.source()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: continue
                val event = parser.parseSseDataLine(line) ?: continue

                if (!event.text.isNullOrBlank()) {
                    generated.append(event.text)
                    emit(LLMStreamEvent.Chunk(event.text))
                }
                if (event.usage != null) usage = event.usage
            }
        }

        val finalUsage = normalizeUsage(
            usage = usage,
            question = question,
            systemPrompt = variant.systemPrompt,
            completionText = generated.toString()
        )
        val metrics = buildMetrics(
            provider = LlmProvider.DEEPSEEK,
            model = request.model,
            latencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1),
            usage = finalUsage
        )
        emit(LLMStreamEvent.Completed(metrics))
    }.flowOn(Dispatchers.IO)

    private fun streamFromOpenRouter(question: String, variant: LLMVariant): Flow<LLMStreamEvent> = flow {
        if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            throw IllegalStateException("OPENROUTER_API_KEY is empty. Add it to local.properties")
        }

        val messages = buildMessages(question, variant)
        val request = ChatCompletionsRequest(
            model = variant.model ?: BuildConfig.DEEPSEEK_MODEL,
            stream = true,
            messages = messages,
            maxTokens = variant.maxTokens,
            topP = variant.topP,
            frequencyPenalty = variant.frequencyPenalty,
            presencePenalty = variant.presencePenalty,
            temperature = variant.temperature,
        )

        val startedAt = System.currentTimeMillis()
        val response = openRouter.chatCompletionsStream(request)

        if (!response.isSuccessful) {
            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            throw IllegalStateException("HTTP $code: ${errorBody.ifBlank { "Request failed" }}")
        }

        val body = response.body() ?: throw IllegalStateException("Empty response body")
        val generated = StringBuilder()
        var usage: TokenUsage? = null

        body.use { responseBody ->
            val source = responseBody.source()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: continue
                val event = parser.parseSseDataLine(line) ?: continue

                if (!event.text.isNullOrBlank()) {
                    generated.append(event.text)
                    emit(LLMStreamEvent.Chunk(event.text))
                }
                if (event.usage != null) usage = event.usage
            }
        }

        val finalUsage = normalizeUsage(
            usage = usage,
            question = question,
            systemPrompt = variant.systemPrompt,
            completionText = generated.toString()
        )
        val metrics = buildMetrics(
            provider = LlmProvider.OPEN_ROUTER,
            model = request.model,
            latencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1),
            usage = finalUsage
        )
        emit(LLMStreamEvent.Completed(metrics))
    }.flowOn(Dispatchers.IO)

    private fun buildMessages(question: String, variant: LLMVariant): List<ChatMessage> {
        return variant.systemPrompt?.let {
            listOf(
                ChatMessage(role = "system", content = it),
                ChatMessage(role = "user", content = question)
            )
        } ?: listOf(ChatMessage(role = "user", content = question))
    }

    private fun normalizeUsage(
        usage: TokenUsage?,
        question: String,
        systemPrompt: String?,
        completionText: String
    ): TokenUsage {
        val estimatedPrompt = estimateTokens(question + (systemPrompt ?: ""))
        val estimatedCompletion = estimateTokens(completionText)

        val prompt = usage?.promptTokens ?: estimatedPrompt
        val completion = usage?.completionTokens ?: estimatedCompletion
        val total = usage?.totalTokens ?: (prompt + completion)

        return TokenUsage(
            promptTokens = prompt,
            completionTokens = completion,
            totalTokens = total
        )
    }

    private fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0
        return (text.length / 4.0).roundToInt().coerceAtLeast(1)
    }

    private fun buildMetrics(
        provider: LlmProvider,
        model: String,
        latencyMs: Long,
        usage: TokenUsage
    ): CompletionMetrics {
        val pricing = pricingFor(provider, model)
        val prompt = usage.promptTokens ?: 0
        val completion = usage.completionTokens ?: 0
        val estimatedCost = (prompt / 1_000_000.0) * pricing.inputPer1MUsd +
            (completion / 1_000_000.0) * pricing.outputPer1MUsd

        return CompletionMetrics(
            provider = provider,
            model = model,
            latencyMs = latencyMs,
            usage = usage,
            estimatedCostUsd = estimatedCost,
            pricingSourceUrl = pricing.sourceUrl
        )
    }

    private fun pricingFor(provider: LlmProvider, model: String): Pricing {
        return when (provider) {
            LlmProvider.DEEPSEEK -> {
                when (model.lowercase()) {
                    "deepseek-chat" -> Pricing(
                        inputPer1MUsd = 0.14,
                        outputPer1MUsd = 0.28,
                        sourceUrl = "https://api-docs.deepseek.com/quick_start/pricing"
                    )

                    "deepseek-reasoner" -> Pricing(
                        inputPer1MUsd = 0.55,
                        outputPer1MUsd = 2.19,
                        sourceUrl = "https://api-docs.deepseek.com/quick_start/pricing"
                    )

                    else -> Pricing(
                        inputPer1MUsd = 0.55,
                        outputPer1MUsd = 2.19,
                        sourceUrl = "https://api-docs.deepseek.com/quick_start/pricing"
                    )
                }
            }

            LlmProvider.OPEN_ROUTER -> Pricing(
                inputPer1MUsd = 0.18,
                outputPer1MUsd = 0.18,
                sourceUrl = "https://openrouter.ai/models"
            )
        }
    }
}
