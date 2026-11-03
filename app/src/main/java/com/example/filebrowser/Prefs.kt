package com.example.filebrowser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

/** SharedPreferences-এ সংরক্ষিত সেটিংস */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var theme: ThemeMode
        get() = ThemeMode.valueOf(sp.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        set(value) { sp.edit().putString("theme", value.name).apply() }

    var showHidden: Boolean
        get() = sp.getBoolean("show_hidden", false)
        set(value) { sp.edit().putBoolean("show_hidden", value).apply() }

    var defaultSort: SortBy
        get() = SortBy.valueOf(sp.getString("sort", SortBy.NAME.name) ?: SortBy.NAME.name)
        set(value) { sp.edit().putString("sort", value.name).apply() }
}

/** UI-তে রিয়েল-টাইম আপডেটের জন্য সেটিংস-এর Compose state */
class SettingsState(private val prefs: Prefs) {
    var theme by mutableStateOf(prefs.theme)
        private set
    var showHidden by mutableStateOf(prefs.showHidden)
        private set
    var sort by mutableStateOf(prefs.defaultSort)
        private set

    fun setTheme(value: ThemeMode) {
        prefs.theme = value
        theme = value
    }

    fun setShowHidden(value: Boolean) {
        prefs.showHidden = value
        showHidden = value
    }

    fun setSort(value: SortBy) {
        prefs.defaultSort = value
        sort = value
    }
}
