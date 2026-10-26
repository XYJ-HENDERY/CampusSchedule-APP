package com.example.campusschedule.ui.course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.model.Course
import kotlinx.coroutines.launch

class AddEditCourseViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CampusApp.instance.repository

    fun save(course: Course, onConflict: (List<Course>) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val conflicts = repo.findConflicts(course)
            if (conflicts.isEmpty()) { repo.upsert(course); onSuccess() }
            else onConflict(conflicts)
        }
    }
}