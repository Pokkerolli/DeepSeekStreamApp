package com.example.deepseekstream.domain.usecase

import com.example.deepseekstream.data.repository.LLMRepository
import com.example.deepseekstream.domain.model.LLMStreamEvent
import com.example.deepseekstream.domain.model.LLMVariant
import kotlinx.coroutines.flow.Flow

class RunDeepSeekTestUseCase(
    private val repository: LLMRepository
) {
    operator fun invoke(question: String, variant: LLMVariant): Flow<String> {
        return repository.streamCompletion(question = question, variant = variant)
    }

    fun streamWithMetrics(question: String, variant: LLMVariant): Flow<LLMStreamEvent> {
        return repository.streamCompletionWithMetrics(question = question, variant = variant)
    }
}
