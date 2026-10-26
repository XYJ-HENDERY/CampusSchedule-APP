package com.example.campusschedule.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromLocalDate(v: LocalDate?): Long? = v?.toEpochDay()
    @TypeConverter fun toLocalDate(v: Long?): LocalDate? = v?.let { LocalDate.ofEpochDay(it) }
}