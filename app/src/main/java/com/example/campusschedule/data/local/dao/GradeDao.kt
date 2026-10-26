package com.example.campusschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.campusschedule.data.model.Grade
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Insert suspend fun insert(grade: Grade): Long
    @Insert suspend fun insertAll(list: List<Grade>)
    @Update suspend fun update(grade: Grade)
    @Delete suspend fun delete(grade: Grade)
    @Query("DELETE FROM grades") suspend fun deleteAll()
    @Query("DELETE FROM grades WHERE semester = :semester") suspend fun deleteBySemester(semester: String)
    @Query("SELECT COUNT(*) FROM grades") suspend fun count(): Int
    @Query("SELECT * FROM grades ORDER BY semester DESC, courseName")
    fun observeAll(): Flow<List<Grade>>
}