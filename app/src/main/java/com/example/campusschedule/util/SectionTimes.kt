package com.example.campusschedule.util

object SectionTimes {
    // 索引0 = 第1节
    val times = listOf(
        "08:00\n08:45", "08:55\n09:40", "10:00\n10:45", "10:55\n11:40",
        "14:00\n14:45", "14:55\n15:40", "16:00\n16:45", "16:55\n17:40",
        "19:00\n19:45", "19:55\n20:40"
    )
    val cnNum = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val total get() = times.size
}