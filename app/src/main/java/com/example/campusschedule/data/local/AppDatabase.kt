package com.example.campusschedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.campusschedule.data.local.dao.BorrowRecordDao
import com.example.campusschedule.data.local.dao.ClassroomDao
import com.example.campusschedule.data.local.dao.CourseDao
import com.example.campusschedule.data.local.dao.GradeDao
import com.example.campusschedule.data.local.dao.UserDao
import com.example.campusschedule.data.model.BorrowRecord
import com.example.campusschedule.data.model.Classroom
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.data.model.Grade
import com.example.campusschedule.data.model.User

@Database(
    entities = [User::class, Course::class, Grade::class, Classroom::class, BorrowRecord::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun gradeDao(): GradeDao
    abstract fun classroomDao(): ClassroomDao
    abstract fun borrowRecordDao(): BorrowRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "campus_schedule.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}