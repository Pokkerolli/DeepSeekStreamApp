package com.example.deepseekstream.presentation.task5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekstream.domain.model.CompletionMetrics
import com.example.deepseekstream.domain.model.LLMStreamEvent
import com.example.deepseekstream.domain.model.LLMVariant
import com.example.deepseekstream.domain.model.LlmProvider
import com.example.deepseekstream.domain.usecase.RunDeepSeekTestUseCase
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Task5ViewModel(
    private val runDeepSeekTestUseCase: RunDeepSeekTestUseCase
) : ViewModel() {

    private companion object {
        const val MODEL_LLAMA = "meta-llama/llama-3.1-8b-instruct"
        const val MODEL_DEEPSEEK_CHAT = "deepseek-chat"
        const val MODEL_DEEPSEEK_REASONER = "deepseek-reasoner"

        // Одинаковые параметры генерации для всех моделей в рамках одного прогона.
        const val COMMON_MAX_TOKENS = 1000
        const val COMMON_TEMPERATURE = 0.4
        const val COMMON_TOP_P = 0.9
        const val COMMON_FREQUENCY_PENALTY = 0.0
        const val COMMON_PRESENCE_PENALTY = 0.0
    }

    private val modelVariants = listOf(
        LLMVariant(
            outputKey = "output1",
            provider = LlmProvider.OPEN_ROUTER,
            model = MODEL_LLAMA,
            systemPrompt = null,
            maxTokens = COMMON_MAX_TOKENS,
            topP = COMMON_TOP_P,
            frequencyPenalty = COMMON_FREQUENCY_PENALTY,
            presencePenalty = COMMON_PRESENCE_PENALTY,
            temperature = COMMON_TEMPERATURE
        ),
        LLMVariant(
            outputKey = "output2",
            provider = LlmProvider.DEEPSEEK,
            model = MODEL_DEEPSEEK_CHAT,
            systemPrompt = null,
            maxTokens = COMMON_MAX_TOKENS,
            topP = COMMON_TOP_P,
            frequencyPenalty = COMMON_FREQUENCY_PENALTY,
            presencePenalty = COMMON_PRESENCE_PENALTY,
            temperature = COMMON_TEMPERATURE
        ),
        LLMVariant(
            outputKey = "output3",
            provider = LlmProvider.DEEPSEEK,
            model = MODEL_DEEPSEEK_REASONER,
            systemPrompt = null,
            maxTokens = COMMON_MAX_TOKENS,
            topP = COMMON_TOP_P,
            frequencyPenalty = COMMON_FREQUENCY_PENALTY,
            presencePenalty = COMMON_PRESENCE_PENALTY,
            temperature = COMMON_TEMPERATURE
        )
    )

    private val comparisonVariant = LLMVariant(
        outputKey = "output4",
        model = MODEL_DEEPSEEK_CHAT,
        systemPrompt = "Ты критик. Сравни 3 ответа по качеству, скорости и ресурсоёмкости. " +
            "Сформулируй ключевые различия между моделями и практический вывод. Определи какая модель в поставленной задаче справилась лучше в разрезе цена/качество" +
            "В конце выдай блок с рабочими ссылками на модели/прайсинг.",
        maxTokens = COMMON_MAX_TOKENS,
        topP = COMMON_TOP_P,
        frequencyPenalty = COMMON_FREQUENCY_PENALTY,
        presencePenalty = COMMON_PRESENCE_PENALTY,
        temperature = COMMON_TEMPERATURE
    )

    private val _state = MutableStateFlow(Task5State())
    val state: StateFlow<Task5State> = _state.asStateFlow()

    private var runJob: Job? = null

    fun onQuestionChanged(value: String) {
        _state.update { it.copy(question = value) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun runTest() {
        val question = state.value.question.trim()
        if (question.isEmpty()) {
            _state.update { it.copy(snackbarMessage = "Введите question") }
            return
        }

        runJob?.cancel()
        runJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    output1 = "",
                    output2 = "",
                    output3 = "",
                    output4 = "",
                    snackbarMessage = null
                )
            }

            val rawResponses = ConcurrentHashMap<String, String>()
            val metricsByOutput = ConcurrentHashMap<String, CompletionMetrics>()

            val jobs = modelVariants.map { variant ->
                async {
                    val rawBuilder = StringBuilder()
                    runDeepSeekTestUseCase.streamWithMetrics(question, variant)
                        .catch { error ->
                            if (error is CancellationException) throw error
                            appendToOutput(variant.outputKey, "\n[ERROR] ${mapError(error)}")
                            _state.update { current ->
                                current.copy(snackbarMessage = "Ошибка в ${variant.outputKey}")
                            }
                        }
                        .collect { event ->
                            when (event) {
                                is LLMStreamEvent.Chunk -> {
                                    rawBuilder.append(event.text)
                                    appendToOutput(variant.outputKey, event.text)
                                }

                                is LLMStreamEvent.Completed -> {
                                    rawResponses[variant.outputKey] = rawBuilder.toString()
                                    metricsByOutput[variant.outputKey] = event.metrics
                                    appendToOutput(
                                        variant.outputKey,
                                        "\n\n${formatMetricsBlock(event.metrics)}"
                                    )
                                }
                            }
                        }
                }
            }

            runCatching { jobs.awaitAll() }

            val comparisonPrompt = buildComparisonPrompt(
                question = question,
                rawResponses = rawResponses,
                metricsByOutput = metricsByOutput
            )

            runDeepSeekTestUseCase.streamWithMetrics(comparisonPrompt, comparisonVariant)
                .catch { error ->
                    if (error is CancellationException) throw error
                    appendToOutput("output4", "\n[ERROR] ${mapError(error)}")
                    _state.update { currentState ->
                        currentState.copy(snackbarMessage = "Ошибка в output4")
                    }
                }
                .collect { event ->
                    when (event) {
                        is LLMStreamEvent.Chunk -> appendToOutput("output4", event.text)
                        is LLMStreamEvent.Completed -> Unit
                    }
                }

            appendToOutput("output4", "\n\nСсылки:\n${linksBlock()}")
            _state.update { it.copy(isRunning = false) }
        }
    }

    fun cancelRun() {
        runJob?.cancel()
        runJob = null
        _state.update {
            it.copy(
                isRunning = false,
                snackbarMessage = "Процесс остановлен"
            )
        }
    }

    private fun buildComparisonPrompt(
        question: String,
        rawResponses: Map<String, String>,
        metricsByOutput: Map<String, CompletionMetrics>
    ): String {
        return buildString {
            appendLine("Вопрос пользователя:")
            appendLine(question)
            appendLine()
            appendLine("output1 (meta-llama/llama-3.1-8b-instruct):")
            appendLine(rawResponses["output1"].orEmpty())
            appendLine(metricsLine(metricsByOutput["output1"]))
            appendLine()
            appendLine("output2 (deepseek-chat):")
            appendLine(rawResponses["output2"].orEmpty())
            appendLine(metricsLine(metricsByOutput["output2"]))
            appendLine()
            appendLine("output3 (deepseek-reasoner):")
            appendLine(rawResponses["output3"].orEmpty())
            appendLine(metricsLine(metricsByOutput["output3"]))
            appendLine()
            appendLine("Сделай структурированное сравнение:")
            appendLine("1) Качество ответа")
            appendLine("2) Скорость")
            appendLine("3) Ресурсоёмкость (токены и стоимость)")
            appendLine("4) Ключевые различия между моделями")
            appendLine("5) Практический вывод, когда какую модель использовать")
            appendLine("6) Добавь ссылки на pricing/model pages")
        }
    }

    private fun metricsLine(metrics: CompletionMetrics?): String {
        if (metrics == null) return "Метрики: недоступны"
        return "Метрики: ${formatLatency(metrics.latencyMs)}, " +
            "prompt=${metrics.usage.promptTokens ?: 0}, " +
            "completion=${metrics.usage.completionTokens ?: 0}, " +
            "total=${metrics.usage.totalTokens ?: 0}, " +
            "cost=\$${formatUsd(metrics.estimatedCostUsd)}"
    }

    private fun formatMetricsBlock(metrics: CompletionMetrics): String {
        return buildString {
            appendLine("Метрики:")
            appendLine("- Время ответа: ${formatLatency(metrics.latencyMs)}")
            appendLine("- Prompt tokens: ${metrics.usage.promptTokens ?: 0}")
            appendLine("- Completion tokens: ${metrics.usage.completionTokens ?: 0}")
            appendLine("- Total tokens: ${metrics.usage.totalTokens ?: 0}")
            append("- Стоимость (оценка): $${formatUsd(metrics.estimatedCostUsd)}")
        }
    }

    private fun formatLatency(latencyMs: Long): String {
        return if (latencyMs >= 1_000) {
            String.format(Locale.US, "%.2f c", latencyMs / 1000.0)
        } else {
            "$latencyMs мс"
        }
    }

    private fun formatUsd(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun linksBlock(): String {
        return buildString {
            appendLine("- DeepSeek pricing: https://api-docs.deepseek.com/quick_start/pricing")
            appendLine("- OpenRouter models/pricing: https://openrouter.ai/models")
            append("- Llama model page: https://openrouter.ai/meta-llama/llama-3.1-8b-instruct")
        }
    }

    private fun appendToOutput(key: String, text: String) {
        _state.update { current ->
            when (key) {
                "output1" -> current.copy(output1 = current.output1 + text)
                "output2" -> current.copy(output2 = current.output2 + text)
                "output3" -> current.copy(output3 = current.output3 + text)
                "output4" -> current.copy(output4 = current.output4 + text)
                else -> current
            }
        }
    }

    private fun mapError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("401") -> "Unauthorized (401). Проверьте API ключ."
            message.contains("timeout", ignoreCase = true) -> "Timeout. Повторите позже."
            message.contains("Unable to resolve host", ignoreCase = true) -> "Нет сети или DNS ошибка."
            else -> message.ifBlank { "Неизвестная ошибка" }
        }
    }

    override fun onCleared() {
        runJob?.cancel()
        super.onCleared()
    }
}
