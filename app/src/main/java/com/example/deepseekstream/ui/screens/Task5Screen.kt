package com.example.deepseekstream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.deepseekstream.presentation.task5.Task5State
import com.example.deepseekstream.presentation.task5.Task5ViewModel
import com.example.deepseekstream.ui.designsystem.AppButton
import com.example.deepseekstream.ui.designsystem.AppGrid2x2
import com.example.deepseekstream.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun Task5Screen(
    viewModel: Task5ViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Task5Content(
        state = state,
        onQuestionChanged = viewModel::onQuestionChanged,
        onRunClick = viewModel::runTest,
        onCancelClick = viewModel::cancelRun,
        onClearSnackbar = viewModel::clearSnackbar
    )
}

private fun countChars(text: String): Int {
    return text.length
}

@Composable
private fun Task5Content(
    state: Task5State,
    onQuestionChanged: (String) -> Unit,
    onRunClick: () -> Unit,
    onCancelClick: () -> Unit,
    onClearSnackbar: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val questionMaxHeight = configuration.screenHeightDp.dp * 0.15f
    var containerHeightPx by remember { mutableIntStateOf(0) }
    var topSectionHeightPx by remember { mutableIntStateOf(0) }
    var questionField by remember { mutableStateOf(TextFieldValue(state.question)) }

    val contentSpacingPx = with(density) { 12.dp.roundToPx() }
    val gridHeight = with(density) {
        (containerHeightPx - topSectionHeightPx - contentSpacingPx)
            .coerceAtLeast(0)
            .toDp()
    }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearSnackbar()
    }

    LaunchedEffect(state.question) {
        if (state.question != questionField.text) {
            questionField = TextFieldValue(state.question)
        }
    }

    val output1Chars = countChars(state.output1)
    val output2Chars = countChars(state.output2)
    val output3Chars = countChars(state.output3)
    val output4Chars = countChars(state.output4)

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
                OutlinedTextField(
                    value = questionField,
                    onValueChange = {
                        questionField = it
                        onQuestionChanged(it.text)
                    },
                    label = { Text("Введите вопрос..") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = questionMaxHeight)
                )

                AppButton(
                    text = if (state.isRunning) "Прервать" else "Запустить тест",
                    onClick = {
                        if (state.isRunning) onCancelClick() else onRunClick()
                    },
                    enabled = true,
                    loading = state.isRunning
                )
            }

            AppGrid2x2(
                items = listOf(
                    "output1: meta-llama/llama-3.1-8b-instruct (символов: $output1Chars)" to state.output1,
                    "output2: deepseek-chat (символов: $output2Chars)" to state.output2,
                    "output3: deepseek-reasoner (символов: $output3Chars)" to state.output3,
                    "output4: сравнение от deepseek-chat (символов: $output4Chars)" to state.output4
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Task5ScreenPreview() {
    AppTheme {
        Task5Content(
            state = Task5State(
                output1 = "Пример output1",
                output2 = "Пример output2",
                output3 = "Пример output3",
            ),
            onQuestionChanged = {},
            onRunClick = {},
            onCancelClick = {},
            onClearSnackbar = {}
        )
    }
}
