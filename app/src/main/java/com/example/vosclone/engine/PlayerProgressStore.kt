package com.example.vosclone.engine

import android.content.Context

data class SongProgress(
    val bestGrade: String,
    val bestScore: Int,
    val mastery: Int
)

data class PlayerProgress(
    val xp: Int = 0,
    val favorites: Set<String> = emptySet(),
    val recentSong: String? = null,
    val songs: Map<String, SongProgress> = emptyMap()
) {
    val level: Int get() = 1 + xp / 1_000
    val levelXp: Int get() = xp % 1_000
}

object PlayerProgressStore {
    private const val PREFS = "brightbeat_progress"

    fun load(context: Context): PlayerProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val songIds = prefs.getStringSet("played_songs", emptySet()).orEmpty()
        return PlayerProgress(
            xp = prefs.getInt("xp", 0),
            favorites = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet(),
            recentSong = prefs.getString("recent_song", null),
            songs = songIds.associateWith { id ->
                SongProgress(
                    bestGrade = prefs.getString("${id}_grade", "-") ?: "-",
                    bestScore = prefs.getInt("${id}_score", 0),
                    mastery = prefs.getInt("${id}_mastery", 0)
                )
            }
        )
    }

    fun markRecent(context: Context, audioFile: String): PlayerProgress {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("recent_song", audioFile).apply()
        return load(context)
    }

    fun toggleFavorite(context: Context, audioFile: String): PlayerProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        if (!favorites.add(audioFile)) favorites.remove(audioFile)
        prefs.edit().putStringSet("favorites", favorites).apply()
        return load(context)
    }

    fun recordCompletion(context: Context, session: GameSession): PlayerProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = session.chart.audioFile
        val previousScore = prefs.getInt("${id}_score", 0)
        val previousMastery = prefs.getInt("${id}_mastery", 0)
        val mastery = maxOf(previousMastery, session.accuracyPercent)
        val grade = gradeFor(mastery)
        val playedSongs = prefs.getStringSet("played_songs", emptySet()).orEmpty().toMutableSet().apply { add(id) }
        val xpAward = 60 + session.accuracyPercent * 2 + session.chart.difficulty.ordinal * 25
        prefs.edit()
            .putInt("xp", prefs.getInt("xp", 0) + xpAward)
            .putString("recent_song", id)
            .putStringSet("played_songs", playedSongs)
            .putString("${id}_grade", grade)
            .putInt("${id}_score", maxOf(previousScore, session.score))
            .putInt("${id}_mastery", mastery)
            .apply()
        return load(context)
    }

    private fun gradeFor(accuracy: Int): String = when {
        accuracy >= 95 -> "S"
        accuracy >= 85 -> "A"
        accuracy >= 70 -> "B"
        accuracy >= 50 -> "C"
        else -> "D"
    }
}
