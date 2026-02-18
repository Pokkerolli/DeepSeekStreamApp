package com.example.deepseekstream.presentation.task2

data class Task2State(
    val question: String = "У вас есть 2 сковородки и 3 котлеты. На приготовление 1 котлеты с одной стороны уходит 1 минута. На одной сковороде вмещается лишь 1 котлета.\n" +
            "Вопрос: за какое минимальное время вы сможете полностью обжарить все 3 котлеты?",
    val output1: String = "",
    val output2: String = "",
    val output3: String = "",
    val output4: String = "",
    val isRunning: Boolean = false,
    val snackbarMessage: String? = null
)
