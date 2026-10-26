package com.example.campusschedule.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.model.Course
import kotlinx.coroutines.launch

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CampusApp.instance.repository
    val courses: LiveData<List<Course>> = repo.observeCourses().asLiveData()
    fun delete(course: Course) = viewModelScope.launch { repo.deleteCourse(course) }
}