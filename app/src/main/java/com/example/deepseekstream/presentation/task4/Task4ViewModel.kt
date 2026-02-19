package com.example.deepseekstream.presentation.task4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekstream.domain.model.DeepSeekVariant
import com.example.deepseekstream.domain.usecase.RunDeepSeekTestUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Task4ViewModel(
    private val runDeepSeekTestUseCase: RunDeepSeekTestUseCase
) : ViewModel() {

    private val temperatureVariants = listOf(
        DeepSeekVariant(
            outputKey = "output1",
            systemPrompt = null,
            maxTokens = 1000,
            temperature = 0.0,
        ),
        DeepSeekVariant(
            outputKey = "output2",
            systemPrompt = null,
            maxTokens = 1000,
            temperature = 0.7,
        ),
        DeepSeekVariant(
            outputKey = "output3",
            systemPrompt = null,
            maxTokens = 1000,
            temperature = 1.2,
        )
    )

    private val comparisonVariant = DeepSeekVariant(
        outputKey = "output4",
        systemPrompt = "Сравни три ответа на один и тот же вопрос при разных temperature. " +
                "Дай краткий анализ отличий по точности, полноте, креативности и риску ошибок. " +
                "В конце дай рекомендацию, какую temperature лучше использовать и для каких задач.",
        maxTokens = 1200,
        temperature = 0.7,
    )

    private val _state = MutableStateFlow(Task4State())
    val state: StateFlow<Task4State> = _state.asStateFlow()

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

            val jobs = temperatureVariants.map { variant ->
                async {
                    runDeepSeekTestUseCase(question, variant)
                        .catch { error ->
                            if (error is CancellationException) throw error
                            appendToOutput(
                                variant.outputKey,
                                "\n[ERROR] ${mapError(error)}"
                            )
                            _state.update { current ->
                                current.copy(snackbarMessage = "Ошибка в ${variant.outputKey}")
                            }
                        }
                        .collect { chunk ->
                            appendToOutput(variant.outputKey, chunk)
                        }
                }
            }

            runCatching { jobs.awaitAll() }

            val current = _state.value
            val comparisonPrompt = buildString {
                appendLine("Вопрос пользователя:")
                appendLine(question)
                appendLine()
                appendLine("Ответ temperature=0:")
                appendLine(current.output1)
                appendLine()
                appendLine("Ответ temperature=0.7:")
                appendLine(current.output2)
                appendLine()
                appendLine("Ответ temperature=1.2:")
                appendLine(current.output3)
                appendLine()
                appendLine("Сделай сравнение и итоговую рекомендацию.")
            }

            runDeepSeekTestUseCase(comparisonPrompt, comparisonVariant)
                .catch { error ->
                    if (error is CancellationException) throw error
                    appendToOutput("output4", "\n[ERROR] ${mapError(error)}")
                    _state.update { currentState ->
                        currentState.copy(snackbarMessage = "Ошибка в output4")
                    }
                }
                .collect { chunk ->
                    appendToOutput("output4", chunk)
                }

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
