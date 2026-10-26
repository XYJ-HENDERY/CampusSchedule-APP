package com.example.campusschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrow_records")
data class BorrowRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val building: String,
    val room: String,
    val capacity: Int,
    val week: Int,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val phone: String,
    val auditor: String,
    val successor: String = "",
    val remark: String,
    val status: String,
    val attachment: String = ""
)