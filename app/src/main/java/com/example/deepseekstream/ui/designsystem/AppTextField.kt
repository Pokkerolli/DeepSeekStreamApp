package com.example.deepseekstream.ui.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
            focusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    )
}
