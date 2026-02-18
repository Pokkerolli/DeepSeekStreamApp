package com.example.deepseekstream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deepseekstream.presentation.task2.Task2ViewModel
import com.example.deepseekstream.ui.designsystem.AppButton
import com.example.deepseekstream.ui.designsystem.AppGrid2x2
import com.example.deepseekstream.ui.designsystem.AppTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun Task2Screen(
    viewModel: Task2ViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val questionMaxHeight = configuration.screenHeightDp.dp * 0.15f
    var containerHeightPx by remember { mutableIntStateOf(0) }
    var topSectionHeightPx by remember { mutableIntStateOf(0) }

    val contentSpacingPx = with(density) { 12.dp.roundToPx() }
    val gridHeight = with(density) {
        (containerHeightPx - topSectionHeightPx - contentSpacingPx)
            .coerceAtLeast(0)
            .toDp()
    }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .onSizeChanged { containerHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.onSizeChanged { topSectionHeightPx = it.height },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppTextField(
                    value = state.question,
                    label = "Введите вопрос..",
                    onValueChange = viewModel::onQuestionChanged,
                    minLines = 2,
                    modifier = Modifier.heightIn(max = questionMaxHeight)
                )

                AppButton(
                    text = if (state.isRunning) "Прервать" else "Запустить тест",
                    onClick = {
                        if (state.isRunning) viewModel.cancelRun() else viewModel.runTest()
                    },
                    enabled = true,
                    loading = state.isRunning
                )
            }

            AppGrid2x2(
                items = listOf(
                    "output1: ответ без доп. инструкций" to state.output1,
                    "output2: пошаговый ответ" to state.output2,
                    "output3: сначала составь промт, потом дай ответ" to state.output3,
                    "output4: рассмотрение вопроса через группу экспертов" to state.output4
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            )
        }
    }
}
