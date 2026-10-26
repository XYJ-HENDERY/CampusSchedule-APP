package com.example.campusschedule.ui.grade

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.model.Grade
import com.example.campusschedule.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class GradeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CampusApp.instance.repository

    val selected = MutableStateFlow(SEM_ALL)

    val grades: LiveData<List<Grade>> =
        combine(repo.observeGrades(), selected) { list, sem ->
            if (sem == SEM_ALL) list else list.filter { it.semester == sem }
        }.asLiveData()

    /** 学号前2位=入学年份,其后 8 个学期,倒序 + "全部" */
    fun semesterOptions(): List<String> {
        val sid = SessionManager.studentId(getApplication())
        val enroll = sid.take(2).toIntOrNull()?.let { 2000 + it } ?: 2024
        val list = (0..7).map { i ->
            val y = enroll + i / 2
            "$y-${y + 1}-${i % 2 + 1}"
        }.reversed()
        return listOf(SEM_ALL) + list
    }

    /** 由学期起始日期推算当前学年学期,如 2026-09 → "2026-2027-1" */
    fun currentSemester(): String {
        val millis = SessionManager.semesterStartMillis(getApplication())
        val date = if (millis > 0)
            java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        else java.time.LocalDate.now()
        val y = date.year
        return if (date.monthValue >= 8) "$y-${y + 1}-1" else "${y - 1}-$y-2"
    }

    fun addGrade(grade: Grade) = viewModelScope.launch { repo.addGrade(grade) }

    fun updateGrade(grade: Grade) = viewModelScope.launch { repo.updateGrade(grade) }

    fun importForSemester(list: List<Grade>, semester: String) =
        viewModelScope.launch { repo.replaceSemesterGrades(list, semester) }

    fun clearSemester(semester: String) = viewModelScope.launch { repo.clearSemester(semester) }

    fun clearAll() = viewModelScope.launch { repo.clearAllGrades() }

    companion object {
        const val SEM_ALL = "全部"
    }
}