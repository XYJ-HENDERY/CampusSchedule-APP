package com.example.campusschedule.ui.classroom

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.model.BorrowRecord
import com.example.campusschedule.data.model.Classroom
import com.example.campusschedule.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ClassroomViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CampusApp.instance.repository
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val sectionPair = MutableStateFlow(1)

    val results: LiveData<List<Classroom>> = combine(
        repo.observeClassrooms(),
        repo.observeCourses(),
        selectedDate,
        sectionPair
    ) { rooms, courses, date, pair ->
        val ctx = getApplication<Application>()
        val week = weekOf(ctx, date)
        val day = date.dayOfWeek.value
        val s1 = pair * 2 - 1
        val s2 = pair * 2
        val busy = courses
            .filter { c ->
                c.dayOfWeek == day &&
                    week in c.startWeek..c.endWeek &&
                    c.startSection <= s2 && c.endSection >= s1
            }
            .map { it.classroom.replace(" ", "") }
            .toSet()
        rooms.filter { "${it.building}${it.room}".replace(" ", "") !in busy }
            .sortedWith(compareBy({ it.building }, { it.room }))
    }.asLiveData()

    fun setDate(date: LocalDate) { selectedDate.value = date }

    fun setSectionPair(pair: Int) { sectionPair.value = pair }

    val records: LiveData<List<BorrowRecord>> = repo.observeBorrowRecords().asLiveData()

    fun submit(record: BorrowRecord) = viewModelScope.launch { repo.submitBorrow(record) }

    private fun weekOf(ctx: Context, date: LocalDate): Int {
        val startMillis = SessionManager.semesterStartMillis(ctx)
        if (startMillis <= 0L) return 1
        val start = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(start, date)
        return if (days < 0) 0 else days.toInt() / 7 + 1
    }
}