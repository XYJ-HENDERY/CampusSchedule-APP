package com.example.campusschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.campusschedule.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert suspend fun insert(user: User): Long
    @Update suspend fun update(user: User)
    @Query("SELECT * FROM users WHERE studentId = :studentId LIMIT 1")
    suspend fun findByStudentId(studentId: String): User?
    @Query("SELECT * FROM users WHERE name = :name AND studentId = :studentId LIMIT 1")
    suspend fun findByNameAndStudentId(name: String, studentId: String): User?
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: Long): Flow<User?>
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?
    @Query("UPDATE users SET phone = :phone WHERE id = :id")
    suspend fun updatePhone(id: Long, phone: String)
}