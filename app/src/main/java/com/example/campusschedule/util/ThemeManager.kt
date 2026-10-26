package com.example.campusschedule.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF = "theme"
    private const val KEY = "night"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isNight(c: Context) = prefs(c).getBoolean(KEY, false)

    fun apply(c: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isNight(c)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun toggle(c: Context) {
        val night = !isNight(c)
        prefs(c).edit().putBoolean(KEY, night).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}