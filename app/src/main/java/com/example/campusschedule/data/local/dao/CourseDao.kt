package com.example.campusschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.campusschedule.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert suspend fun insert(course: Course): Long
    @Update suspend fun update(course: Course)
    @Delete suspend fun delete(course: Course)
    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    fun observeAll(): Flow<List<Course>>
    @Query("UPDATE courses SET startWeek = 1, endWeek = 1 WHERE teacher = '' AND startWeek = 1 AND endWeek = 20")
    suspend fun fixSeedToWeek1Only()
    @Query("DELETE FROM courses WHERE teacher = ''")
    suspend fun deleteSeeded()
    @Query("DELETE FROM courses")
    suspend fun deleteAll()
    @Query(
        """
        SELECT * FROM courses
        WHERE id != :excludeId AND dayOfWeek = :day
          AND startSection <= :endSection AND endSection >= :startSection
          AND NOT (:endWeek < startWeek OR :startWeek > endWeek)
        """
    )
    suspend fun findConflicts(
        day: Int, startSection: Int, endSection: Int,
        startWeek: Int, endWeek: Int, excludeId: Long
    ): List<Course>
}