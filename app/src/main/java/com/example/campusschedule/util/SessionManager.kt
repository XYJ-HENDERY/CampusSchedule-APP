package com.example.campusschedule.util

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object SessionManager {
    private const val PREF = "session"
    private const val KEY_UID = "uid"
    private const val KEY_NAME = "name"
    private const val KEY_STUDENT_ID = "studentId"
    private const val KEY_SEM_START = "semesterStart"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun save(context: Context, uid: Long, name: String, studentId: String, semesterStart: Long) {
        prefs(context).edit()
            .putLong(KEY_UID, uid).putString(KEY_NAME, name)
            .putString(KEY_STUDENT_ID, studentId).putLong(KEY_SEM_START, semesterStart)
            .apply()
    }

    fun isLoggedIn(context: Context) = prefs(context).getLong(KEY_UID, 0L) > 0L
    fun uid(context: Context) = prefs(context).getLong(KEY_UID, 0L)
    fun userName(context: Context) = prefs(context).getString(KEY_NAME, "") ?: ""
    fun studentId(context: Context) = prefs(context).getString(KEY_STUDENT_ID, "") ?: ""
    fun logout(context: Context) = prefs(context).edit().clear().apply()

    fun currentWeek(context: Context): Int {
        val startMillis = prefs(context).getLong(KEY_SEM_START, 0L)
        if (startMillis <= 0L) return 1
        val start = java.time.Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(start, LocalDate.now())
        return if (days < 0) 1 else (days.toInt() / 7 + 1)
    }

    fun semesterStartMillis(context: Context): Long =
        prefs(context).getLong(KEY_SEM_START, 0L)
}