# DeepSeekStreamApp

Android-приложение на Kotlin + Jetpack Compose для демонстрации двух заданий:
1. `Task1`: простой экран `Hello, world!`
2. `Task2`: запуск 4 параллельных stream-запросов в DeepSeek и вывод результатов в сетке `2x2`

## Технологии

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Jetpack Compose + Material 3
- Koin (DI)
- Retrofit + OkHttp
- Kotlinx Serialization
- Coroutines + Flow

## Требования

- Android Studio (актуальная версия с поддержкой AGP 8.7.x)
- JDK 17
- Android SDK:
  - `compileSdk = 35`
  - `targetSdk = 35`
  - `minSdk = 24`

## Настройка API ключа

Добавьте ключ в `local.properties` в корне проекта:

```properties
DEEPSEEK_API_KEY=your_real_key_here
```

Ключ передается в `BuildConfig.DEEPSEEK_API_KEY` через `buildConfigField`.

## Запуск

Из корня проекта:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Или через Android Studio: открыть папку `DeepSeekStreamApp` и запустить модуль `app`.

## Что делает Task2

- Отправляет 4 параллельных запроса на `POST https://api.deepseek.com/chat/completions`
- Использует модель `deepseek-chat` и `stream=true`
- Для каждого `outputX` применяет свой системный промпт и `max_tokens`:
  - `output1`: без system prompt, `max_tokens = 1000`
  - `output2`: пошаговое решение, `max_tokens = 1000`
  - `output3`: сначала сформировать промпт, затем дать результат, `max_tokens = 2000`
  - `output4`: подход через группу экспертов, `max_tokens = 3000`
- Показывает потоковый текст по мере получения SSE-чанков
- Поддерживает отмену процесса кнопкой `Прервать`

## Реализация стриминга

- Поток читается построчно из `ResponseBody.source().readUtf8Line()`
- Обрабатываются строки формата `data: ...` и маркер завершения `data: [DONE]`
- Текст извлекается из:
  - `choices[0].delta.content`
  - fallback: `choices[0].message.content`
- Каждый фрагмент сразу добавляется в соответствующий `output1..output4`

## Структура экранов

- `StartScreen` - переход к заданиям
- `Task1Screen` - статический `Hello, world!`
- `Task2Screen` - поле вопроса, кнопка запуска/остановки, 4 результата в сетке
