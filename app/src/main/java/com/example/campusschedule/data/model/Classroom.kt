package com.example.campusschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classrooms")
data class Classroom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val building: String,
    val room: String,
    val capacity: Int,
    val availableTime: String
)
