package com.example.vosclone.ui.menu

import android.content.Context

/** Home catalogue presentations available to the player. */
enum class HomeLayoutMode {
    CLASSIC,
    COMPACT
}

object HomeLayoutModeStore {
    private const val PREFS = "brightbeat_preferences"
    private const val KEY_HOME_LAYOUT = "home_layout_mode"

    /** Classic is intentionally the first-run default for new players. */
    fun load(context: Context): HomeLayoutMode {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HOME_LAYOUT, HomeLayoutMode.CLASSIC.name)
        return runCatching { HomeLayoutMode.valueOf(value ?: HomeLayoutMode.CLASSIC.name) }
            .getOrDefault(HomeLayoutMode.CLASSIC)
    }

    fun save(context: Context, mode: HomeLayoutMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOME_LAYOUT, mode.name)
            .apply()
    }
}
