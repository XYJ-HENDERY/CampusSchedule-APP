package com.example.campusschedule.data.repository

import android.content.Context
import com.example.campusschedule.CampusApp
import com.example.campusschedule.data.local.dao.BorrowRecordDao
import com.example.campusschedule.data.local.dao.ClassroomDao
import com.example.campusschedule.data.local.dao.CourseDao
import com.example.campusschedule.data.local.dao.GradeDao
import com.example.campusschedule.data.local.dao.UserDao
import com.example.campusschedule.data.model.BorrowRecord
import com.example.campusschedule.data.model.Classroom
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.data.model.Grade
import com.example.campusschedule.data.model.User
import com.example.campusschedule.data.model.gradeTextToGpa
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val userDao: UserDao,
    private val courseDao: CourseDao,
    private val gradeDao: GradeDao,
    private val classroomDao: ClassroomDao,
    private val borrowRecordDao: BorrowRecordDao
) {
    suspend fun register(name: String, studentId: String, password: String, semesterStart: Long): Long =
        userDao.insert(User(name = name, studentId = studentId, password = password, semesterStart = semesterStart))

    suspend fun findByStudentId(studentId: String): User? = userDao.findByStudentId(studentId)

    suspend fun getUser(id: Long): User? = userDao.getById(id)

    suspend fun updatePhone(uid: Long, phone: String) = userDao.updatePhone(uid, phone)

    suspend fun login(studentId: String, password: String): User? =
        userDao.findByStudentId(studentId)?.takeIf { it.password == password }

    suspend fun resetPassword(name: String, studentId: String, newPassword: String): Boolean {
        val user = userDao.findByNameAndStudentId(name, studentId) ?: return false
        userDao.update(user.copy(password = newPassword))
        return true
    }

    fun observeCourses(): Flow<List<Course>> = courseDao.observeAll()

    suspend fun upsert(course: Course): Long =
        if (course.id == 0L) courseDao.insert(course) else { courseDao.update(course); course.id }

    suspend fun deleteCourse(course: Course) = courseDao.delete(course)

    suspend fun findConflicts(course: Course): List<Course> =
        courseDao.findConflicts(
            day = course.dayOfWeek, startSection = course.startSection,
            endSection = course.endSection, startWeek = course.startWeek,
            endWeek = course.endWeek, excludeId = course.id
        )

    suspend fun importCourses(list: List<Course>) {
        courseDao.deleteAll()
        list.forEach { courseDao.insert(it.copy(id = 0)) }
    }

    fun observeGrades(): Flow<List<Grade>> = gradeDao.observeAll()

    suspend fun addGrade(grade: Grade): Long = gradeDao.insert(grade)

    suspend fun deleteGrade(grade: Grade) = gradeDao.delete(grade)

    suspend fun updateGrade(grade: Grade) = gradeDao.update(grade)

    /** 只替换指定学期的成绩,其他学期保持不变 */
    suspend fun replaceSemesterGrades(list: List<Grade>, semester: String) {
        gradeDao.deleteBySemester(semester)
        list.forEach { gradeDao.insert(it.copy(id = 0, semester = semester)) }
    }

    suspend fun clearSemester(semester: String) = gradeDao.deleteBySemester(semester)

    suspend fun clearAllGrades() = gradeDao.deleteAll()

    suspend fun seedGradesIfEmpty() {
        if (gradeDao.count() == 0) gradeDao.insertAll(demoGrades())
    }

    fun observeClassrooms(): Flow<List<Classroom>> = classroomDao.observeAll()

    fun observeBorrowRecords(): Flow<List<BorrowRecord>> = borrowRecordDao.observeAll()
    suspend fun submitBorrow(record: BorrowRecord): Long = borrowRecordDao.insert(record)

    suspend fun seedWeek1CoursesIfEmpty() {
        val prefs = CampusApp.instance.getSharedPreferences("seed", Context.MODE_PRIVATE)
        val targetVersion = 25
        val v = prefs.getInt("seed_version", 0)
        val empty = courseDao.count() == 0
        if (v >= targetVersion && !empty) return
        if (empty) {
            defaultWeek1Courses().forEach { courseDao.insert(it) }
        } else {
            courseDao.deleteSeeded()
            defaultWeek1Courses().forEach { courseDao.insert(it) }
        }
        prefs.edit().putInt("seed_version", targetVersion).apply()
    }

    suspend fun seedClassroomsIfEmpty() {
        if (classroomDao.count() == 0) {
            classroomDao.insertAll(defaultClassrooms())
        }
    }

    companion object {
        private const val BLUE = 0xFF1E88E5.toInt()
        private const val GREEN = 0xFF43A047.toInt()
        private const val ORANGE = 0xFFFB8C00.toInt()
        private const val PURPLE = 0xFF8E24AA.toInt()
        private const val RED = 0xFFE53935.toInt()
        private const val YELLOW = 0xFFFDD835.toInt()

        private fun defaultClassrooms() = listOf(
            // ===== 第一教学楼 1F =====
            Classroom(building = "第一教学楼", room = "101", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "102", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "103", capacity = 80, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "104", capacity = 80, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "105", capacity = 120, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "106", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "107", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "108", capacity = 120, availableTime = "08:00-21:00"),
            // ===== 第一教学楼 2F =====
            Classroom(building = "第一教学楼", room = "201", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "202", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "203", capacity = 80, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "204", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "205", capacity = 120, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "206", capacity = 40, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "207", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "208", capacity = 120, availableTime = "08:00-21:00"),
            // ===== 第一教学楼 3F =====
            Classroom(building = "第一教学楼", room = "301", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "302", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "303", capacity = 40, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "304", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "305", capacity = 80, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "306", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "307", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "308", capacity = 120, availableTime = "08:00-21:00"),
            // ===== 第一教学楼 4F =====
            Classroom(building = "第一教学楼", room = "401", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "402", capacity = 40, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "403", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "404", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "405", capacity = 120, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "406", capacity = 40, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "407", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "408", capacity = 120, availableTime = "08:00-21:00"),
            // ===== 第一教学楼 5F =====
            Classroom(building = "第一教学楼", room = "501", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "502", capacity = 40, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "503", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "504", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "505", capacity = 120, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "506", capacity = 200, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "507", capacity = 60, availableTime = "08:00-21:00"),
            Classroom(building = "第一教学楼", room = "508", capacity = 120, availableTime = "08:00-21:00"),
        )

        private fun demoGrades() = listOf(
            Grade(semester = "2024-2025-1", courseName = "中国古建筑欣赏与设计", courseCode = "000004764", credit = 2.0, property = "公选", gradeText = "良"),
            Grade(semester = "2024-2025-1", courseName = "社会实践", courseCode = "2000016010", credit = 1.0, property = "必修", gradeText = "优"),
            Grade(semester = "2024-2025-2", courseName = "大学体育(4)", courseCode = "2000018010", credit = 1.0, property = "必修", gradeText = "87"),
            Grade(semester = "2025-2026-1", courseName = "高级Web技术", courseCode = "2001331025", credit = 2.5, property = "选修", gradeText = "89"),
            Grade(semester = "2025-2026-1", courseName = "操作系统", courseCode = "2001337040", credit = 4.0, property = "必修", gradeText = "91"),
            Grade(semester = "2025-2026-2", courseName = "数据通信技术", courseCode = "2001375030", credit = 3.0, property = "选修", gradeText = "97"),
            Grade(semester = "2025-2026-2", courseName = "操作系统课程设计", courseCode = "2001777020", credit = 2.0, property = "必修", gradeText = "85"),
            Grade(semester = "2026-2027-1", courseName = "综合实践能力创新实训(1)", courseCode = "2001785020", credit = 2.0, property = "必修", gradeText = "92"),
            Grade(semester = "2026-2027-1", courseName = "毛泽东思想和中国特色社会主义理论体系概论", courseCode = "2200169030", credit = 3.0, property = "必修", gradeText = "81"),
            Grade(semester = "2026-2027-1", courseName = "石文化与宝玉石鉴赏", courseCode = "2400002620", credit = 2.0, property = "公选", gradeText = "良"),
        ).map { it.copy(gpa = gradeTextToGpa(it.gradeText)) }

        private fun defaultWeek1Courses() = listOf(
            // ===== 第 1~8 周 =====
            Course(name = "射频识别与传感器技术A", teacher = "", classroom = "第一教学楼205", dayOfWeek = 3, startSection = 7, endSection = 8, startWeek = 1, endWeek = 8, color = RED, type = "必修"),
            Course(name = "智能终端系统及应用开发", teacher = "", classroom = "第一教学楼505", dayOfWeek = 4, startSection = 7, endSection = 8, startWeek = 1, endWeek = 8, color = ORANGE, type = "必修"),
            Course(name = "射频识别与传感器技术A", teacher = "", classroom = "第一教学楼405", dayOfWeek = 5, startSection = 5, endSection = 6, startWeek = 1, endWeek = 8, color = RED, type = "必修"),

            // ===== 第 1~12 周（软件工程两门已结课）=====
            Course(name = "软件工程", teacher = "", classroom = "第一教学楼108", dayOfWeek = 2, startSection = 1, endSection = 2, startWeek = 1, endWeek = 12, color = GREEN, type = "必修"),
            Course(name = "软件工程", teacher = "", classroom = "第一教学楼105", dayOfWeek = 4, startSection = 3, endSection = 4, startWeek = 1, endWeek = 12, color = GREEN, type = "必修"),

            // ===== 第 1~14 周 =====
            Course(name = "计算机网络", teacher = "", classroom = "第一教学楼308", dayOfWeek = 1, startSection = 1, endSection = 2, startWeek = 1, endWeek = 14, color = BLUE, type = "必修"),
            Course(name = "计算机网络", teacher = "", classroom = "第一教学楼408", dayOfWeek = 3, startSection = 3, endSection = 4, startWeek = 1, endWeek = 14, color = BLUE, type = "必修"),

            // ===== 第 1~5 周 =====
            Course(name = "创业基础", teacher = "", classroom = "第一教学楼203", dayOfWeek = 3, startSection = 5, endSection = 6, startWeek = 1, endWeek = 5, color = PURPLE, type = "必修"),

            // ===== 第 1~7 周 =====
            Course(name = "智能终端系统及应用开发", teacher = "", classroom = "第一教学楼505", dayOfWeek = 2, startSection = 3, endSection = 4, startWeek = 1, endWeek = 7, color = ORANGE, type = "必修"),

            // ===== 第 5~8 周 =====
            Course(name = "智能终端系统及应用开发", teacher = "", classroom = "逸夫楼433(网络工程)", dayOfWeek = 1, startSection = 5, endSection = 8, startWeek = 5, endWeek = 8, color = ORANGE, type = "必修"),
            Course(name = "射频识别与传感器技术A", teacher = "", classroom = "逸夫楼533(物联网工程)", dayOfWeek = 5, startSection = 1, endSection = 4, startWeek = 5, endWeek = 8, color = RED, type = "必修"),

            // ===== 仅第 8 周 =====
            Course(name = "智能终端系统及应用开发", teacher = "", classroom = "第一教学楼108", dayOfWeek = 2, startSection = 9, endSection = 10, startWeek = 8, endWeek = 8, color = ORANGE, type = "必修"),

            // ===== 第 9~14 周 =====
            Course(name = "汇编语言程序设计", teacher = "", classroom = "第一教学楼405", dayOfWeek = 1, startSection = 3, endSection = 4, startWeek = 9, endWeek = 14, color = YELLOW, type = "必修"),
            Course(name = "汇编语言程序设计", teacher = "", classroom = "第一教学楼205", dayOfWeek = 3, startSection = 7, endSection = 8, startWeek = 9, endWeek = 14, color = YELLOW, type = "必修"),

            // ===== 第 9~11 周 =====
            Course(name = "创业基础", teacher = "", classroom = "第一教学楼203", dayOfWeek = 3, startSection = 5, endSection = 6, startWeek = 9, endWeek = 11, color = PURPLE, type = "必修"),

            // ===== 第 11~14 周 =====
            Course(name = "计算机网络", teacher = "", classroom = "第一教学楼205", dayOfWeek = 4, startSection = 7, endSection = 8, startWeek = 11, endWeek = 14, color = BLUE, type = "必修"),

            // ===== 第 12~14 周 =====
            Course(name = "汇编语言程序设计", teacher = "", classroom = "逸夫楼537(计算机应用技术)", dayOfWeek = 2, startSection = 3, endSection = 4, startWeek = 12, endWeek = 14, color = YELLOW, type = "必修"),

            // ===== 第 13~14 周 =====
            Course(name = "形势与政策", teacher = "", classroom = "第一教学楼308", dayOfWeek = 5, startSection = 3, endSection = 4, startWeek = 13, endWeek = 14, color = RED, type = "必修"),

            // ===== 仅第 14 周 =====
            Course(name = "汇编语言程序设计", teacher = "", classroom = "逸夫楼537(计算机应用技术)", dayOfWeek = 4, startSection = 1, endSection = 2, startWeek = 14, endWeek = 14, color = YELLOW, type = "必修")
        )
    }
}