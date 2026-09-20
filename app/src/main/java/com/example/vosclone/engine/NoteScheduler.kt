package com.example.vosclone.engine

import com.example.vosclone.chart.Chart
import com.example.vosclone.chart.ChartNote
import kotlin.math.roundToLong

/**
 * How far ahead of its hit-time (in ms) a note becomes visible on screen.
 * This, combined with the render lane's pixel height, determines fall speed:
 *   speedPxPerMs = laneHeightPx / LOOKAHEAD_MS
 */
const val LOOKAHEAD_MS = 1800L

class NoteScheduler(chart: Chart) {

    private val sourceChart = chart

    /** Mutable because placeholder tracks can be extended after audio duration is known. */
    val runtimeNotes: MutableList<RuntimeNote> = chart.notes
        .sortedBy { it.timeMs }
        .map { RuntimeNote(it) }
        .toMutableList()

    /** Number of deterministic placeholder notes added to cover an incomplete chart. */
    var generatedFallbackCount: Int = 0
        private set

    /**
     * Detects a chart that stops well before the audio track and fills the gap with
     * a BPM-grid placeholder. This keeps the demo playable while real charts are
     * being authored; it is intentionally deterministic so the same song behaves
     * the same way on every device.
     *
     * Returns the number of generated notes. Calling it repeatedly is safe.
     */
    fun extendToAudioDuration(audioDurationMs: Long): Int {
        if (generatedFallbackCount > 0 || audioDurationMs <= 0L) return generatedFallbackCount

        val beatMs = (60_000.0 / sourceChart.bpm.coerceAtLeast(30)).roundToLong().coerceAtLeast(120L)
        val lastChartTime = runtimeNotes.maxOfOrNull { it.note.timeMs }
        val coverageGapMs = audioDurationMs - (lastChartTime ?: 0L)
        val failureThresholdMs = maxOf(4L * beatMs, 4_000L)

        // A chart close to the end of the track is considered authored and left alone.
        if (runtimeNotes.isNotEmpty() && coverageGapMs <= failureThresholdMs) return 0

        val firstGeneratedTime = (lastChartTime?.plus(beatMs) ?: (2L * beatMs)).coerceAtLeast(beatMs)
        val finalHitTime = (audioDurationMs - 350L).coerceAtLeast(firstGeneratedTime)
        // Keep the fallback relaxed and readable: one generated hit per beat
        // at most, instead of the denser half-beat stream used previously.
        val subdivisionMs = beatMs
        val measureMs = subdivisionMs * 4L
        val rhythmPatterns = listOf(
            intArrayOf(0, 2, 3),
            intArrayOf(0, 1, 3),
            intArrayOf(0, 2),
            intArrayOf(0, 1, 2, 3),
            intArrayOf(0, 3)
        )
        var measureStart = firstGeneratedTime
        var measureIndex = 0
        var generated = 0

        // Rotate syncopated patterns and lanes instead of repeating a 1-2-3-4 walk.
        while (measureStart <= finalHitTime) {
            val pattern = rhythmPatterns[Math.floorMod(measureIndex + sourceChart.title.length, rhythmPatterns.size)]
            pattern.forEachIndexed { patternIndex, step ->
                val hitTime = measureStart + (step * subdivisionMs)
                if (hitTime <= finalHitTime) {
                    val laneCount = sourceChart.lanes.coerceAtLeast(1)
                    val lane = Math.floorMod(
                        sourceChart.title.hashCode() + measureIndex * 3 + patternIndex * 5 + step * 7,
                        laneCount
                    )
                    val isHold = patternIndex == 1 && measureIndex % 4 == 2
                    runtimeNotes += RuntimeNote(
                        ChartNote(
                            lane = lane,
                            timeMs = hitTime,
                            type = if (isHold) "hold" else "tap",
                            durationMs = if (isHold) (beatMs * 2L) else 0L
                        )
                    )
                    generated += 1
                }
            }
            measureIndex += 1
            measureStart += measureMs
        }

        if (generated > 0) {
            runtimeNotes.sortBy { it.note.timeMs }
            generatedFallbackCount = generated
        }
        return generatedFallbackCount
    }

    /** Notes currently on screen (within the lookahead window and not yet resolved). */
    fun visibleNotes(currentTimeMs: Long, timingOffsetMs: Long = 0L): List<RuntimeNote> =
        runtimeNotes.filter { rn ->
            val startTime = rn.note.timeMs + timingOffsetMs
            val endTime = startTime + rn.note.durationMs
            (rn.state == NoteState.PENDING || rn.state == NoteState.HOLDING) &&
                startTime - currentTimeMs <= LOOKAHEAD_MS &&
                endTime - currentTimeMs >= -TimingWindows.MISS_MS
        }

    /**
     * Call once per frame: any pending note whose hit-time has passed the miss
     * window is auto-missed. Returns the notes newly marked MISSED this call
     * so the caller can update score/combo for each one exactly once.
     */
    fun autoMissExpiredNotes(currentTimeMs: Long, timingOffsetMs: Long = 0L): List<RuntimeNote> {
        val newlyMissed = mutableListOf<RuntimeNote>()
        runtimeNotes.forEach { rn ->
            val targetTime = rn.note.timeMs + timingOffsetMs
            if (rn.state == NoteState.PENDING && currentTimeMs - targetTime > TimingWindows.MISS_MS) {
                rn.state = NoteState.MISSED
                newlyMissed.add(rn)
            }
        }
        return newlyMissed
    }

    /**
     * Finds the best PENDING candidate note in [lane] for a tap at [tapTimeMs],
     * i.e. the closest one still within the miss window, so a tap resolves the
     * note it was most likely aimed at.
     */
    fun findTapTarget(lane: Int, tapTimeMs: Long, timingOffsetMs: Long = 0L): RuntimeNote? =
        runtimeNotes
            .filter { it.state == NoteState.PENDING && it.note.lane == lane }
            .minByOrNull { kotlin.math.abs((it.note.timeMs + timingOffsetMs) - tapTimeMs) }
            ?.takeIf { kotlin.math.abs((it.note.timeMs + timingOffsetMs) - tapTimeMs) <= TimingWindows.MISS_MS }

    fun isChartComplete(currentTimeMs: Long, timingOffsetMs: Long = 0L): Boolean =
        runtimeNotes.all { it.state == NoteState.HIT || it.state == NoteState.MISSED } &&
            currentTimeMs > ((runtimeNotes.maxOfOrNull { it.note.timeMs } ?: 0L) + timingOffsetMs)
}
