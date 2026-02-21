package com.example.deepseekstream.data.network

import com.example.deepseekstream.data.model.ChatCompletionsRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenRouterApi {
    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionsStream(
        @Body request: ChatCompletionsRequest
    ): Response<ResponseBody>
}