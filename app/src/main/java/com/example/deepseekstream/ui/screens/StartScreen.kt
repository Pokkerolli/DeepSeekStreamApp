package com.example.deepseekstream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deepseekstream.ui.designsystem.AppButton

@Composable
fun StartScreen(
    onTask1Click: () -> Unit,
    onTask2Click: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppButton(
            text = "Первое задание",
            onClick = onTask1Click,
            enabled = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppButton(
            text = "Второе задание",
            onClick = onTask2Click,
            enabled = true
        )
    }
}
