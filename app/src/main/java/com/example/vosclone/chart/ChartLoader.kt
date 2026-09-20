package com.example.vosclone.chart

import android.content.Context
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

object ChartLoader {

    /** Loads a chart bundled in app/src/main/assets/charts/<fileName>. */
    fun loadFromAssets(context: Context, fileName: String): Chart {
        val text = context.assets.open("charts/$fileName").bufferedReader().use { it.readText() }
        return json.decodeFromString(Chart.serializer(), text)
    }

    /** Serializes a chart, e.g. one produced by the in-app editor, for local storage. */
    fun toJson(chart: Chart): String = json.encodeToString(Chart.serializer(), chart)

    /** Parses chart JSON text, e.g. loaded from local app storage after editing. */
    fun fromJson(text: String): Chart = json.decodeFromString(Chart.serializer(), text)

    /** Loads every bundled song, supplying a deterministic playable chart when no JSON is authored yet. */
    fun loadBundledCatalogue(context: Context, songCount: Int = 21): List<Chart> =
        (1..songCount).map { index ->
            val authored = runCatching { loadFromAssets(context, "demo$index.json") }.getOrNull()
            val difficulty = ChartDifficulty.entries[(index - 1) % ChartDifficulty.entries.size]
            val owned = index <= 8
            (authored ?: generatedChart(index, difficulty, owned)).copy(
                difficulty = difficulty,
                level = 2 + ((index * 3) % 13),
                stars = (1 + ((index - 1) % 5)),
                packId = if (owned) "Starter Set" else "Neon Nights",
                owned = owned
            )
        }

    private fun generatedChart(index: Int, difficulty: ChartDifficulty, owned: Boolean): Chart {
        val bpm = 104 + ((index * 7) % 48)
        val beatMs = 60_000L / bpm
        val step = when (difficulty) {
            ChartDifficulty.EASY -> beatMs
            ChartDifficulty.NORMAL -> beatMs * 3L / 4L
            ChartDifficulty.HARD -> beatMs / 2L
        }
        val notes = buildList {
            var time = 1_800L
            var noteIndex = 0
            while (time < 105_000L) {
                val isHold = noteIndex % (10 - difficulty.ordinal * 2) == 7
                add(
                    ChartNote(
                        lane = (noteIndex * 3 + index) % 4,
                        timeMs = time,
                        type = if (isHold) "hold" else "tap",
                        durationMs = if (isHold) beatMs else 0L
                    )
                )
                time += step
                noteIndex++
            }
        }
        return Chart(
            title = "BrightBeat Track $index",
            artist = "BrightBeat Artist",
            audioFile = "demo$index.mp3",
            bpm = bpm,
            difficulty = difficulty,
            level = 2 + ((index * 3) % 13),
            stars = 1 + ((index - 1) % 5),
            packId = if (owned) "Starter Set" else "Neon Nights",
            owned = owned,
            notes = notes
        )
    }
}
