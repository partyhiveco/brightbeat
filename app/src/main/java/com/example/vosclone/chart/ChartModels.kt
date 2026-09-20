package com.example.vosclone.chart

import kotlinx.serialization.Serializable

/**
 * A single note in a chart.
 * timeMs   - the audio playback timestamp (ms) at which this note should be hit.
 * lane     - which of the [Chart.lanes] columns this note falls in (0-indexed).
 * type     - "tap" for a single hit, "hold" for a sustained note.
 * durationMs - only used when type == "hold"; how long the hold lasts.
 */
@Serializable
data class ChartNote(
    val lane: Int,
    val timeMs: Long,
    val type: String = "tap",
    val durationMs: Long = 0L
)

/**
 * A full chart: metadata + the ordered list of notes.
 * audioFile refers to a filename under assets/audio/ (for bundled demo songs)
 * or a content:// / file:// URI (for user-imported songs via the editor).
 */
@Serializable
data class Chart(
    val title: String,
    val artist: String = "Unknown",
    val audioFile: String,
    val bpm: Int,
    val lanes: Int = 4,
    val offsetMs: Long = 0L,
    val notes: List<ChartNote>
)
