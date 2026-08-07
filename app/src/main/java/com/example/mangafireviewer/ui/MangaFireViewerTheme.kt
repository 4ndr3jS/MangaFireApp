package com.example.mangafireviewer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MangaFireDarkColors = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color(0xFF2D0B00),
    background = Color(0xFF101114),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF181A1F),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF252830),
    onSurfaceVariant = Color(0xFFC7CAD1),
    error = Color(0xFFFFB4AB),
)

@Composable
fun MangaFireViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MangaFireDarkColors,
        content = content,
    )
}
