package com.example.vosclone.engine

import com.example.vosclone.chart.ChartNote

enum class NoteState { PENDING, HOLDING, HIT, MISSED }

/**
 * Mutable per-note runtime state, separate from the immutable ChartNote data
 * class so we can track hit/miss without mutating the parsed chart itself.
 */
data class RuntimeNote(
    val note: ChartNote,
    var state: NoteState = NoteState.PENDING,
    var startJudgement: Judgement? = null,
    /** Audio-clock time at which the player actually started the hold. */
    var holdStartedAtMs: Long = 0L,
    /** Next 140 ms subdivision that should award a hold tick. */
    var nextHoldTickMs: Long = Long.MAX_VALUE,
    /** Allows a short finger drift/re-entry grace period before breaking. */
    var lostContactSinceMs: Long = -1L
)
