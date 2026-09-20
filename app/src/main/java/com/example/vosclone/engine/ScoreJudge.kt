package com.example.vosclone.engine

enum class Judgement { PERFECT, GOOD, MISS }

/**
 * Timing windows (ms) around a note's target hit time.
 * Tune these to taste - tighter windows = harder game.
 */
object TimingWindows {
    const val PERFECT_MS = 30L
    const val GOOD_MS = 80L
    const val MISS_MS = 150L // beyond this, the tap doesn't count as an attempt on the note at all
}

object ScoreJudge {

    /**
     * Compares the moment the player tapped a lane ([tapTimeMs]) against a
     * note's scheduled hit time ([noteTimeMs]) and returns how well-timed it was.
     */
    fun judge(tapTimeMs: Long, noteTimeMs: Long): Judgement {
        val delta = kotlin.math.abs(tapTimeMs - noteTimeMs)
        return when {
            delta <= TimingWindows.PERFECT_MS -> Judgement.PERFECT
            delta <= TimingWindows.GOOD_MS -> Judgement.GOOD
            else -> Judgement.MISS
        }
    }

    fun pointsFor(judgement: Judgement): Int = when (judgement) {
        Judgement.PERFECT -> 100
        Judgement.GOOD -> 50
        Judgement.MISS -> 0
    }
}
