package com.example.deepseekstream.presentation.task2

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

class Task2ViewModel(
    private val runDeepSeekTestUseCase: RunDeepSeekTestUseCase
) : ViewModel() {

    private val variants = listOf(
        DeepSeekVariant(
            outputKey = "output1",
            systemPrompt = null,
            maxTokens = 1000,
        ),
        DeepSeekVariant(
            outputKey = "output2",
            systemPrompt = "Решай задачу пошагово. После напиши \"Ответ: \" и дай сразу ответ",
            maxTokens = 1000,
        ),
        DeepSeekVariant(
            outputKey = "output3",
            systemPrompt = "Ты — модель, работающая в два этапа при решении любой пользовательской задачи.\n" +
                    "Обязательный порядок действий:\n" +
                    "Сначала сформируй внутренний промпт, который наилучшим образом поможет решить поставленную задачу.\n" +
                    "Промпт должен быть чётким, структурированным и оптимизированным для получения качественного результата.\n" +
                    "Укажи в нём цель, ограничения, формат ответа и критерии качества.\n" +
                    "Затем используй созданный промпт, чтобы сгенерировать финальный ответ пользователю.\n" +
                    "В финальном выводе:\n" +
                    "Сначала выведи созданный промпт (с пометкой Сформированный промпт:).\n" +
                    "После этого выведи результат его выполнения (с пометкой Результат:).\n" +
                    "Правила:\n" +
                    "Не пропускай этап генерации промпта.\n" +
                    "Не смешивай этапы — сначала промпт, затем выполнение.\n" +
                    "Промпт должен быть универсальным и понятным.\n" +
                    "Итоговый ответ должен строго соответствовать сформированному промпту.",
            maxTokens = 2000,
        ),
        DeepSeekVariant(
            outputKey = "output4",
            systemPrompt = "Ты — система коллективного аналитического мышления.\n" +
                    "При получении задачи действуй строго по шагам:\n" +
                    "1. Формирование экспертов\n" +
                    "Создай 3 экспертов:\n" +
                    "Логик — проверяет корректность рассуждений и выводов.\n" +
                    "Аналитик — структурирует данные, выявляет закономерности.\n" +
                    "Критик — ищет ошибки, допущения и слабые места.\n" +
                    "Кратко обозначь их роль.\n" +
                    "2. Независимое решение\n" +
                    "Каждый эксперт отдельно решает задачу пошагово.\n" +
                    "Рассуждения должны быть логичными, без пропусков.\n" +
                    "Если данных недостаточно — явно укажи это.\n" +
                    "3. Проверка и синтез\n" +
                    "Критик анализирует решения других.\n" +
                    "Исправь выявленные ошибки.\n" +
                    "Представь финальное согласованное решение.\n" +
                    "Формат ответа\n" +
                    "Эксперты\n" +
                    "Решения\n" +
                    "Критический разбор\n" +
                    "Итоговое решение\n" +
                    "Правила:\n" +
                    "Чёткая логика\n" +
                    "Пошаговые выводы\n" +
                    "Без лишних рассуждений\n" +
                    "Итог должен быть строго обоснован",
            maxTokens = 3000,
        )
    )

    private val _state = MutableStateFlow(Task2State())
    val state: StateFlow<Task2State> = _state.asStateFlow()

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

            val jobs = variants.map { variant ->
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
