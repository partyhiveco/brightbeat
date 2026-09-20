package com.example.vosclone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A single, high-impact sans family makes the signal-pop direction legible
 * while weight and tracking create the display/utility hierarchy.
 */
private val DisplaySans = FontFamily.SansSerif
private val UiSans = FontFamily.SansSerif

val Logotype = TextStyle(
    fontFamily = DisplaySans,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 56.sp,
    letterSpacing = (-2).sp
)

val ScreenTitle = TextStyle(
    fontFamily = DisplaySans,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp
)

val SongTitle = TextStyle(
    fontFamily = DisplaySans,
    fontWeight = FontWeight.Bold,
    fontSize = 19.sp,
    letterSpacing = 0.2.sp
)

val GradeLetter = TextStyle(
    fontFamily = DisplaySans,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 96.sp
)

val AppTypography = Typography(
    headlineLarge = Logotype,
    headlineMedium = ScreenTitle,
    titleLarge = SongTitle,
    bodyLarge = TextStyle(fontFamily = UiSans, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = UiSans, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
)
