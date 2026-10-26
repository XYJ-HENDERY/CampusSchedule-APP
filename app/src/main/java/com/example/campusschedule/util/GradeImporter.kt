package com.example.campusschedule.util

import com.example.campusschedule.data.model.Grade
import com.example.campusschedule.data.model.gradeTextToGpa

/** 解析教务导出的成绩单文件(xlsx / docx / XML Spreadsheet / HTML) */
object GradeImporter {

    private val props = setOf("必修", "选修", "公选", "限选")
    private val gradeWords = setOf("优", "优秀", "良", "中", "及格", "不及格")

    fun parse(bytes: ByteArray, defaultSemester: String): List<Grade> {
        val grid = ScheduleImporter.parseGrid(bytes)

        // ---- 模式一:表格式(有「课程/成绩」表头,一行一门课) ----
        tableFormat(grid, defaultSemester)?.let { return it }

        // ---- 模式二:词流块状(名称/编号/学分/属性/成绩 五元组滑动匹配) ----
        val joined = grid.flatten().flatMap { it.split("\n") }.joinToString(" ")
        val tokens = Regex("[\\s·•\\u3000]+").split(joined).filter { it.isNotEmpty() }
        val result = mutableListOf<Grade>()
        var i = 0
        while (i + 4 < tokens.size) {
            val name = tokens[i]
            val code = tokens[i + 1]
            val credit = tokens[i + 2].toDoubleOrNull()
            val prop = tokens[i + 3]
            val score = tokens[i + 4]
            if (name.length >= 2 && !name.contains(":") && !name.contains("：") &&
                !name.contains("学期") && !name.contains("绩点") &&
                code.matches(Regex("\\d{8,12}")) &&
                credit != null && credit in 0.0..20.0 && prop in props && isScoreText(score)
            ) {
                result += Grade(
                    semester = defaultSemester, courseName = name, courseCode = code,
                    credit = credit, property = prop, gradeText = score,
                    gpa = gradeTextToGpa(score)
                )
                i += 5
            } else {
                i++
            }
        }
        if (result.isNotEmpty()) return result

        throw ScheduleImporter.ImportFormatException("未能从文件中识别成绩数据")
    }

    private fun tableFormat(grid: List<List<String>>, defaultSemester: String): List<Grade>? {
        var header = -1
        for ((ri, row) in grid.withIndex()) {
            val hasCourse = row.any { it.contains("课程") && !it.contains("编号") && !it.contains("属性") }
            val hasScore = row.any {
                (it.contains("成绩") || it.contains("分数") || it.contains("等级")) && !it.contains("课程") && !it.contains("绩点")
            }
            if (hasCourse && hasScore) { header = ri; break }
        }
        if (header < 0) return null

        val h = grid[header]
        fun col(vararg keys: String): Int =
            h.indexOfFirst { c -> keys.any { c.replace(" ", "").contains(it) } }

        val nameCol = col("课程名称", "课程")
        val codeCol = col("编号", "代码")
        val creditCol = col("学分")
        val propCol = col("属性", "性质")
        val gpaCol = col("绩点")
        val scoreCol = col("成绩", "分数", "等级")
        val semCol = col("学期")

        val result = mutableListOf<Grade>()
        for (row in grid.drop(header + 1)) {
            fun at(i: Int) = row.getOrNull(i)?.trim() ?: ""
            val name = at(nameCol)
            val score = at(scoreCol)
            if (name.isEmpty() || name.contains("课程")) continue
            if (score.isEmpty()) continue
            result += Grade(
                semester = normalizeSemester(at(semCol)) ?: defaultSemester,
                courseName = name,
                courseCode = at(codeCol),
                credit = numAt(at(creditCol)) ?: 0.0,
                property = at(propCol),
                gradeText = score,
                gpa = numAt(at(gpaCol)) ?: gradeTextToGpa(score)
            )
        }
        return result.ifEmpty { null }
    }

    private fun isScoreText(s: String): Boolean =
        s in gradeWords || (s.toDoubleOrNull()?.let { it in 0.0..100.0 } == true)

    private fun numAt(s: String): Double? =
        Regex("(\\d+(\\.\\d+)?)").find(s)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun normalizeSemester(s: String): String? {
        if (s.isBlank()) return null
        Regex("(\\d{4})\\s*[-–—~]\\s*(\\d{4}).*?第?\\s*([一二12])\\s*学?期?").find(s)?.let {
            val n = when (it.groupValues[3]) { "二" -> "2"; "一" -> "1"; else -> it.groupValues[3] }
            return "${it.groupValues[1]}-${it.groupValues[2]}-$n"
        }
        return null
    }
}