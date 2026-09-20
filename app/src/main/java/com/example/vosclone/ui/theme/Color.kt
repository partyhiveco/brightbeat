package com.example.vosclone.ui.theme

import androidx.compose.ui.graphics.Color

// Pop/beat palette: deep concert navy with saturated candy light accents.
// The names used by gameplay remain stable, so this is a visual-only direction change.
val Ink = Color(0xFF07102E)
val InkElevated = Color(0xFF0D1740)
val InkElevated2 = Color(0xFF142550)
val Ivory = Color(0xFFF7F4FF)
val IvoryMuted = Color(0xFFB5BFDF)
val Brass = Color(0xFFFFDD3A)
val BrassDim = Color(0xFFB9A42A)
val SignalOrange = Color(0xFFFFB547)
val SignalRed = Color(0xFFFF2A91)
val SignalTeal = Color(0xFF3BE8FF)
val SignalBlue = Color(0xFF6C8CFF)
val Scanline = Color(0x22DDE7FF)

// Explicit pop names make the visual intent clear on redesigned screens.
val PopPink = Color(0xFFFF2A91)
val PopCyan = Color(0xFF3BE8FF)
val PopLavender = Color(0xFF9B7CFF)
val PopYellow = Color(0xFFFFDD3A)

// Lane colors mapped to orchestra sections rather than an arbitrary rainbow -
// a deliberate choice tied to the game's subject matter (Virtual Orchestra).
val Strings = SignalRed
val BrassLane = Brass
val Woodwind = SignalTeal
val Percussion = SignalBlue

val LaneColors = listOf(Strings, BrassLane, Woodwind, Percussion)
val LaneNames = listOf("Strings", "Brass", "Woodwind", "Percussion")

// Judgement feedback colors
val JudgePerfect = Brass
val JudgeGood = SignalOrange
val JudgeMiss = SignalRed
