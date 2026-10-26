package com.example.campusschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.campusschedule.data.model.BorrowRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowRecordDao {
    @Insert
    suspend fun insert(record: BorrowRecord): Long
    @Query("SELECT * FROM borrow_records ORDER BY id DESC")
    fun observeAll(): Flow<List<BorrowRecord>>
}