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
}
