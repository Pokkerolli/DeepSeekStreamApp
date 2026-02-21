package com.example.deepseekstream.di

import com.example.deepseekstream.BuildConfig
import com.example.deepseekstream.data.network.DeepSeekApi
import com.example.deepseekstream.data.network.OpenRouterApi
import com.example.deepseekstream.data.parser.StreamingParser
import com.example.deepseekstream.data.repository.LLMRepository
import com.example.deepseekstream.domain.usecase.RunDeepSeekTestUseCase
import com.example.deepseekstream.presentation.task2.Task2ViewModel
import com.example.deepseekstream.presentation.task4.Task4ViewModel
import com.example.deepseekstream.presentation.task5.Task5ViewModel
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

private const val DEEPSEEK_RETROFIT = "DEEPSEEK_RETROFIT"
private const val OPEN_ROUTER_RETROFIT = "OPEN_ROUTER_RETROFIT"

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    single {
        Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")

            if (original.url.host.contains("api.deepseek.com") && original.header("Authorization") == null) {
                requestBuilder.addHeader("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
            }
            if (original.url.host.contains("openrouter.ai") && original.header("Authorization") == null) {
                requestBuilder.addHeader("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .retryOnConnectionFailure(true)
            .build()
    }

    single(named("plainClient")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .retryOnConnectionFailure(true)
            .build()
    }

    single(named(DEEPSEEK_RETROFIT)) {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    single(named(OPEN_ROUTER_RETROFIT)) {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BuildConfig.OPENROUTER_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    single<DeepSeekApi> { get<Retrofit>(named(DEEPSEEK_RETROFIT)).create(DeepSeekApi::class.java) }
    single<OpenRouterApi> { get<Retrofit>(named(OPEN_ROUTER_RETROFIT)).create(OpenRouterApi::class.java) }
}

val repositoryModule = module {
    single { StreamingParser(get()) }
    single { LLMRepository(get(), get(), get()) }
}

val useCaseModule = module {
    factory { RunDeepSeekTestUseCase(get()) }
}

val viewModelModule = module {
    viewModel { Task2ViewModel(get()) }
    viewModel { Task4ViewModel(get()) }
    viewModel { Task5ViewModel(get()) }
}

val appModules = listOf(
    networkModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
