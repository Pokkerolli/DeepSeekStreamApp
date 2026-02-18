package com.example.deepseekstream.domain.usecase

import com.example.deepseekstream.data.repository.DeepSeekRepository
import com.example.deepseekstream.domain.model.DeepSeekVariant
import kotlinx.coroutines.flow.Flow

class RunDeepSeekTestUseCase(
    private val repository: DeepSeekRepository
) {
    operator fun invoke(question: String, variant: DeepSeekVariant): Flow<String> {
        return repository.streamCompletion(question = question, variant = variant)
    }
}
