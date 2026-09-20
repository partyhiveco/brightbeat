package com.example.vosclone.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.vosclone.chart.Chart
import com.example.vosclone.chart.ChartNote

enum class FeedbackKind { JUDGEMENT, HOLD_TICK, HOLD_COMPLETE }

data class FeedbackEvent(
    val kind: FeedbackKind,
    val lane: Int,
    val judgement: Judgement? = null,
    /** False when the pointer handler already fired the immediate tap pulse. */
    val haptic: Boolean = false
)

/**
 * Holds mutable Compose state for one play-through: score, combo, and the
 * most recent judgement (used to flash "PERFECT"/"GOOD"/"MISS" feedback).
 * Kept separate from NoteScheduler so scoring logic doesn't need to know
 * about note-visibility/timing-window bookkeeping.
 */
class GameSession(
    val chart: Chart,
    /** Positive values move the chart target later to compensate for latency. */
    val timingOffsetMs: Long = 0L
) {

    val scheduler = NoteScheduler(chart)

    /** Chart-authored offset plus the device calibration offset. */
    val totalTimingOffsetMs: Long get() = chart.offsetMs + timingOffsetMs

    var score by mutableIntStateOf(0)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var maxCombo by mutableIntStateOf(0)
        private set
    var lastJudgement by mutableStateOf<Judgement?>(null)
        private set
    /** Monotonic event id so repeated identical hits still animate in Compose. */
    var judgementSerial by mutableIntStateOf(0)
        private set
    /** Lane associated with the latest judgement, or -1 for an auto-miss. */
    var lastJudgementLane by mutableIntStateOf(-1)
        private set
    /** Monotonic serial for all visual/tactile feedback, including hold ticks. */
    var feedbackSerial by mutableLongStateOf(0L)
        private set
    var perfectCount by mutableIntStateOf(0)
        private set
    var goodCount by mutableIntStateOf(0)
        private set
    var missCount by mutableIntStateOf(0)
        private set

    private val activeHolds = mutableMapOf<Int, RuntimeNote>()
    private val feedbackEvents = ArrayDeque<FeedbackEvent>()

    companion object {
        const val HOLD_TICK_INTERVAL_MS = 140L
        const val HOLD_TICK_POINTS = 2
        const val HOLD_DRIFT_GRACE_MS = 110L
    }

    val totalNotes: Int get() = scheduler.runtimeNotes.size

    /** Weighted accuracy: perfects count fully, goods count half, misses count zero. */
    val accuracyPercent: Int
        get() {
            if (totalNotes == 0) return 100
            val weighted = perfectCount + (goodCount * 0.5)
            return ((weighted / totalNotes) * 100).toInt()
        }

    fun targetTimeMs(note: ChartNote): Long = note.timeMs + totalTimingOffsetMs

    /** Returns and clears feedback events produced since the previous UI frame. */
    fun drainFeedbackEvents(): List<FeedbackEvent> {
        if (feedbackEvents.isEmpty()) return emptyList()
        val events = feedbackEvents.toList()
        feedbackEvents.clear()
        return events
    }

    fun onTap(lane: Int, tapTimeMs: Long): Judgement? {
        val target = scheduler.findTapTarget(lane, tapTimeMs, totalTimingOffsetMs) ?: return null
        val judgement = ScoreJudge.judge(tapTimeMs, targetTimeMs(target.note))
        target.startJudgement = judgement
        if (target.note.type.equals("hold", ignoreCase = true) && judgement != Judgement.MISS) {
            target.state = NoteState.HOLDING
            target.holdStartedAtMs = tapTimeMs.coerceAtLeast(targetTimeMs(target.note))
            target.nextHoldTickMs = target.holdStartedAtMs + HOLD_TICK_INTERVAL_MS
            target.lostContactSinceMs = -1L
            activeHolds[lane] = target
        } else {
            target.state = NoteState.HIT
        }
        registerJudgement(judgement, lane)
        return judgement
    }

    /** True while a finger should remain down to complete a hold note. */
    fun hasActiveHold(lane: Int): Boolean = activeHolds.containsKey(lane)

    /** Completes or breaks the active hold on a lane when its pointer is released. */
    fun releaseHold(lane: Int, releaseTimeMs: Long) {
        val hold = activeHolds.remove(lane) ?: return
        if (releaseTimeMs + TimingWindows.GOOD_MS >= targetTimeMs(hold.note) + hold.note.durationMs) {
            completeHold(hold)
        } else {
            breakHold(hold)
        }
    }

    /** Called from the audio-clock loop to complete holds even before pointer-up. */
    fun tickHolds(currentTimeMs: Long, heldLanes: Set<Int>) {
        val completed = activeHolds.entries.toList()
        completed.forEach { (lane, hold) ->
            val endTimeMs = targetTimeMs(hold.note) + hold.note.durationMs
            if (lane in heldLanes) {
                hold.lostContactSinceMs = -1L
                while (currentTimeMs >= hold.nextHoldTickMs && hold.nextHoldTickMs < endTimeMs) {
                    score += HOLD_TICK_POINTS
                    combo += 1
                    maxCombo = maxOf(maxCombo, combo)
                    enqueueFeedback(FeedbackKind.HOLD_TICK, lane)
                    hold.nextHoldTickMs += HOLD_TICK_INTERVAL_MS
                }
                if (currentTimeMs >= endTimeMs) {
                    completeHold(hold)
                    activeHolds.remove(lane)
                }
            } else if (currentTimeMs >= endTimeMs) {
                breakHold(hold)
                activeHolds.remove(lane)
            } else {
                if (hold.lostContactSinceMs < 0L) hold.lostContactSinceMs = currentTimeMs
                if (currentTimeMs - hold.lostContactSinceMs > HOLD_DRIFT_GRACE_MS) {
                    breakHold(hold)
                    activeHolds.remove(lane)
                }
            }
        }
    }

    /** Progress of the active hold on [lane], used to render a live progress cue. */
    fun holdProgressForLane(lane: Int, currentTimeMs: Long): Float? {
        val hold = activeHolds[lane] ?: return null
        if (hold.note.durationMs <= 0L) return 1f
        return ((currentTimeMs - targetTimeMs(hold.note)).toFloat() / hold.note.durationMs.toFloat())
            .coerceIn(0f, 1f)
    }

    private fun completeHold(hold: RuntimeNote) {
        if (hold.state != NoteState.HOLDING) return
        hold.state = NoteState.HIT
        score += (hold.note.durationMs / 40L).toInt().coerceAtLeast(25)
        enqueueFeedback(FeedbackKind.HOLD_COMPLETE, hold.note.lane)
    }

    private fun breakHold(hold: RuntimeNote) {
        if (hold.state != NoteState.HOLDING) return
        hold.state = NoteState.MISSED
        when (hold.startJudgement) {
            Judgement.PERFECT -> {
                perfectCount -= 1
                score -= ScoreJudge.pointsFor(Judgement.PERFECT)
            }
            Judgement.GOOD -> {
                goodCount -= 1
                score -= ScoreJudge.pointsFor(Judgement.GOOD)
            }
            else -> Unit
        }
        combo = 0
        missCount += 1
        score = score.coerceAtLeast(0)
        lastJudgement = Judgement.MISS
        lastJudgementLane = hold.note.lane
        judgementSerial += 1
        enqueueFeedback(FeedbackKind.JUDGEMENT, hold.note.lane, Judgement.MISS, haptic = true)
    }

    /** Call once per frame to auto-miss any notes the player let pass, updating score/combo accordingly. */
    fun tickAutoMiss(currentTimeMs: Long) {
        val newlyMissed = scheduler.autoMissExpiredNotes(currentTimeMs, totalTimingOffsetMs)
        newlyMissed.forEach { registerJudgement(Judgement.MISS, it.note.lane) }
    }

    private fun registerJudgement(judgement: Judgement, lane: Int = -1) {
        lastJudgement = judgement
        lastJudgementLane = lane
        judgementSerial += 1
        when (judgement) {
            Judgement.PERFECT -> perfectCount += 1
            Judgement.GOOD -> goodCount += 1
            Judgement.MISS -> missCount += 1
        }
        if (judgement == Judgement.MISS) {
            combo = 0
        } else {
            combo += 1
            maxCombo = maxOf(maxCombo, combo)
        }
        score += ScoreJudge.pointsFor(judgement)
        enqueueFeedback(FeedbackKind.JUDGEMENT, lane, judgement)
    }

    private fun enqueueFeedback(
        kind: FeedbackKind,
        lane: Int,
        judgement: Judgement? = null,
        haptic: Boolean = kind != FeedbackKind.JUDGEMENT
    ) {
        feedbackEvents.addLast(FeedbackEvent(kind, lane, judgement, haptic))
        feedbackSerial += 1L
    }
}
