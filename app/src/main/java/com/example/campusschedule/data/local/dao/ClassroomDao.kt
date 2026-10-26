package com.example.campusschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.campusschedule.data.model.Classroom
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Classroom>)
    @Query("SELECT * FROM classrooms")
    fun observeAll(): Flow<List<Classroom>>
    @Query("SELECT COUNT(*) FROM classrooms")
    suspend fun count(): Int
}