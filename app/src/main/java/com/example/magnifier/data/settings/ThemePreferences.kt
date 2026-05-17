package com.example.magnifier.data.settings

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.magnifier.ui.theme.ThemeSpec

// SharedPreferences-backed store for the user's chosen theme.
// Exposes the selection as Compose State so the root composable
// re-renders the whole MaterialTheme when the user picks a new theme.
//
// SharedPreferences (not DataStore) because we need to read the initial
// value synchronously before composition — DataStore is async-first and
// would either force an initial flash of Default or a coroutine in init.
class ThemePreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _current = mutableStateOf(
        ThemeSpec.fromId(prefs.getString(KEY_THEME_ID, ThemeSpec.Default.id) ?: ThemeSpec.Default.id)
    )
    val current: State<ThemeSpec> get() = _current

    fun select(spec: ThemeSpec) {
        if (_current.value.id == spec.id) return
        _current.value = spec
        prefs.edit().putString(KEY_THEME_ID, spec.id).apply()
    }

    companion object {
        private const val PREFS_NAME = "magnifier_settings"
        private const val KEY_THEME_ID = "current_theme_id"
    }
}
