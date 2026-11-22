package dev.tasker2.data

import android.content.Context
import dev.tasker2.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Minimal int key-value storage so persistence can be tested without Android. */
interface IntStore {
    fun getInt(key: String, default: Int): Int
    fun putInts(values: Map<String, Int>)
}

class SharedPrefsIntStore(context: Context) : IntStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInts(values: Map<String, Int>) {
        prefs.edit().apply { values.forEach { (k, v) -> putInt(k, v) } }.apply()
    }

    companion object { const val FILE = "tasker2_settings" }
}

/** Persists [AppSettings]; [settings] updates immediately so the UI applies changes live. */
class SettingsRepository(private val store: IntStore) {
    private val state = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> get() = state

    private fun load(): AppSettings = AppSettings(
        tabFontSp = store.getInt(KEY_TAB_FONT, AppSettings.DEFAULT_TAB_FONT_SP),
        listFontSp = store.getInt(KEY_LIST_FONT, AppSettings.DEFAULT_LIST_FONT_SP),
        tabGapSpaces = store.getInt(KEY_TAB_GAP, AppSettings.DEFAULT_TAB_GAP_SPACES),
    ).normalized()

    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(state.value).normalized()
        store.putInts(mapOf(KEY_TAB_FONT to next.tabFontSp, KEY_LIST_FONT to next.listFontSp, KEY_TAB_GAP to next.tabGapSpaces))
        state.value = next
    }

    fun reset() = update { AppSettings() }

    companion object {
        const val KEY_TAB_FONT = "tab_font_sp"
        const val KEY_LIST_FONT = "list_font_sp"
        const val KEY_TAB_GAP = "tab_gap_spaces"
    }
}
