package com.example.deepseekstream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CodexDarkColors = darkColorScheme(
    primary = CodexPrimary,
    onPrimary = CodexOnPrimary,
    background = CodexBg,
    onBackground = CodexText,
    surface = CodexSurface,
    onSurface = CodexText,
    surfaceVariant = CodexSurfaceVariant,
    onSurfaceVariant = CodexTextMuted,
    outline = CodexBorder
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CodexDarkColors,
        typography = AppTypography,
        content = content
    )
}
