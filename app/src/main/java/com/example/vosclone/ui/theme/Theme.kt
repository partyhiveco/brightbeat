package com.example.vosclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VosColorScheme = darkColorScheme(
    background = Ink,
    surface = InkElevated,
    surfaceVariant = InkElevated2,
    primary = Brass,
    onPrimary = Ink,
    onBackground = Ivory,
    onSurface = Ivory,
    secondary = BrassDim,
    error = JudgeMiss
)

@Composable
fun VosCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VosColorScheme,
        typography = AppTypography,
        content = content
    )
}
