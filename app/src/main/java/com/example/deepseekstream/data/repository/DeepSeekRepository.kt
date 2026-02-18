package com.example.deepseekstream.data.repository

import com.example.deepseekstream.BuildConfig
import com.example.deepseekstream.data.model.ChatCompletionsRequest
import com.example.deepseekstream.data.model.ChatMessage
import com.example.deepseekstream.data.network.DeepSeekApi
import com.example.deepseekstream.data.parser.StreamingParser
import com.example.deepseekstream.domain.model.DeepSeekVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ensureActive

class DeepSeekRepository(
    private val api: DeepSeekApi,
    private val parser: StreamingParser
) {
    fun streamCompletion(question: String, variant: DeepSeekVariant): Flow<String> = flow {
        if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) {
            throw IllegalStateException("DEEPSEEK_API_KEY is empty. Add it to local.properties")
        }

        val messages = variant.systemPrompt?.let {
            listOf(
                ChatMessage(role = "system", content = it),
                ChatMessage(role = "user", content = question)
            )
        } ?: listOf(ChatMessage(role = "user", content = question))

        val request = ChatCompletionsRequest(
            model = BuildConfig.DEEPSEEK_MODEL,
            stream = true,
            messages = messages,
            maxTokens = variant.maxTokens,
        )

        val response = api.chatCompletionsStream(request)

        if (!response.isSuccessful) {
            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            throw IllegalStateException("HTTP $code: ${errorBody.ifBlank { "Request failed" }}")
        }

        val body = response.body() ?: throw IllegalStateException("Empty response body")

        body.use { responseBody ->
            val source = responseBody.source()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: continue
                val chunk = parser.extractTextFromSseDataLine(line)
                if (!chunk.isNullOrBlank()) emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)
}
