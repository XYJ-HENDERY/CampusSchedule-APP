package com.example.campusschedule.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.model.User
import java.time.LocalDate
import java.time.ZoneId

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CampusApp.instance.repository
    private val studentIdRegex = Regex("^\\d{10}$")

    fun login(studentId: String, password: String) = liveData<Result<User>> {
        when {
            !studentIdRegex.matches(studentId) -> emit(Result.failure(Exception("学号必须是10位数字")))
            password.isBlank() -> emit(Result.failure(Exception("请输入密码")))
            else -> {
                val user = repo.login(studentId, password)
                if (user == null) emit(Result.failure(Exception("学号或密码错误")))
                else emit(Result.success(user))
            }
        }
    }

    fun register(name: String, studentId: String, password: String, confirm: String) = liveData<Result<Long>> {
        val realName = name.trim()
        when {
            realName.isBlank() -> emit(Result.failure(Exception("请输入真实姓名")))
            !studentIdRegex.matches(studentId) -> emit(Result.failure(Exception("学号必须是10位数字")))
            password.isBlank() -> emit(Result.failure(Exception("请输入密码")))
            password != confirm -> emit(Result.failure(Exception("两次输入的密码不一致")))
            repo.findByStudentId(studentId) != null -> emit(Result.failure(Exception("该学号已被注册")))
            else -> {
                val semesterStart = LocalDate.of(2026, 9, 7)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                emit(Result.success(repo.register(realName, studentId, password, semesterStart)))
            }
        }
    }

    fun resetPassword(name: String, studentId: String, newPassword: String, confirm: String) = liveData<Result<Boolean>> {
        val realName = name.trim()
        when {
            realName.isBlank() -> emit(Result.failure(Exception("请输入真实姓名")))
            !studentIdRegex.matches(studentId) -> emit(Result.failure(Exception("学号必须是10位数字")))
            newPassword.isBlank() -> emit(Result.failure(Exception("请输入新密码")))
            newPassword != confirm -> emit(Result.failure(Exception("两次输入的密码不一致")))
            !repo.resetPassword(realName, studentId, newPassword) ->
                emit(Result.failure(Exception("姓名与学号不匹配，重置失败")))
            else -> emit(Result.success(true))
        }
    }
}