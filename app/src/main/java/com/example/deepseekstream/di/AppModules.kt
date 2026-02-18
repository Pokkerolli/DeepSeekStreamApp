package com.example.deepseekstream.di

import com.example.deepseekstream.BuildConfig
import com.example.deepseekstream.data.network.DeepSeekApi
import com.example.deepseekstream.data.parser.StreamingParser
import com.example.deepseekstream.data.repository.DeepSeekRepository
import com.example.deepseekstream.domain.usecase.RunDeepSeekTestUseCase
import com.example.deepseekstream.presentation.task2.Task2ViewModel
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

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
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
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

    single {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    single<DeepSeekApi> {
        get<Retrofit>().create(DeepSeekApi::class.java)
    }
}

val repositoryModule = module {
    single { StreamingParser(get()) }
    single { DeepSeekRepository(get(), get()) }
}

val useCaseModule = module {
    factory { RunDeepSeekTestUseCase(get()) }
}

val viewModelModule = module {
    viewModel { Task2ViewModel(get()) }
}

val appModules = listOf(
    networkModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
