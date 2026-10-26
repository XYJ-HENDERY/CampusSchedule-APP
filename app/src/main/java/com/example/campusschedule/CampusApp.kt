package com.example.campusschedule

import android.app.Application
import com.example.campusschedule.data.local.AppDatabase
import com.example.campusschedule.data.repository.ScheduleRepository
import com.example.campusschedule.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CampusApp : Application() {
    lateinit var repository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.apply(this)
        val db = AppDatabase.get(this)
        repository = ScheduleRepository(
            db.userDao(), db.courseDao(), db.gradeDao(), db.classroomDao(), db.borrowRecordDao()
        )
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedClassroomsIfEmpty()
        }
    }

    companion object {
        lateinit var instance: CampusApp
            private set
    }
}
