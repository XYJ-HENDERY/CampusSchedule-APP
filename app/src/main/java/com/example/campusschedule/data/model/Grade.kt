package com.example.campusschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semester: String,        // "2024-2025-1"
    val courseName: String,
    val courseCode: String = "",
    val credit: Double = 0.0,
    val property: String = "",   // 必修/选修/公选
    val gradeText: String,       // "87" 或 优/良/中/及格
    val gpa: Double = 0.0        // 绩点
)

/** 成绩文本 → 绩点(4.0 分制满分) */
fun gradeTextToGpa(text: String): Double = when (text.trim()) {
    "优", "优秀" -> 4.0
    "良" -> 3.0
    "中" -> 2.0
    "及格" -> 1.0
    "不及格" -> 0.0
    else -> {
        val s = text.trim().toDoubleOrNull() ?: return 0.0
        when {
            s >= 90 -> 4.0
            s >= 80 -> 3.0 + (s - 80) / 10
            s >= 70 -> 2.0 + (s - 70) / 10
            s >= 60 -> 1.0 + (s - 60) / 10
            else -> 0.0
        }
    }
}