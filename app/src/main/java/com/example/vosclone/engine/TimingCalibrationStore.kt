package com.example.vosclone.engine

import android.content.Context

/** Persists the player's audio/touch compensation on this device only. */
object TimingCalibrationStore {
    private const val PREFS_NAME = "vos_timing"
    private const val KEY_OFFSET_MS = "offset_ms"
    const val MIN_OFFSET_MS = -250L
    const val MAX_OFFSET_MS = 250L

    fun load(context: Context): Long = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(KEY_OFFSET_MS, 0L)
        .coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)

    fun save(context: Context, offsetMs: Long) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_OFFSET_MS, offsetMs.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS))
            .apply()
    }
}
