package com.example.deepseekstream.presentation.task5

data class Task5State(
    val question: String = "",
    val output1: String = "",
    val output2: String = "",
    val output3: String = "",
    val output4: String = "",
    val isRunning: Boolean = false,
    val snackbarMessage: String? = null
)