package com.example.deepseekstream.presentation.task4

data class Task4State(
    val question: String = "",
    val output1: String = "",
    val output2: String = "",
    val output3: String = "",
    val output4: String = "",
    val isRunning: Boolean = false,
    val snackbarMessage: String? = null
)