package com.example.campusschedule.ui.schedule

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.util.SectionTimes
import java.time.LocalDate

object ScheduleGridBuilder {

    private const val LEFT_W = 46f
    private const val CELL_H = 52f
    private const val DIV_H = 16f
    private const val HEADER_H = 46f
    private const val GRID_PAD = 4f
    private val dividers = intArrayOf(4, 8)

    fun buildWeekHeader(context: Context, row: LinearLayout, displayedWeek: Int, currentWeek: Int, baseMonday: LocalDate) {
        row.removeAllViews()
        val displayedMonday = baseMonday.plusWeeks((displayedWeek - currentWeek).toLong())
        val today = LocalDate.now()
        val corner = TextView(context)
        corner.layoutParams = LinearLayout.LayoutParams(dp(context, LEFT_W), dp(context, HEADER_H))
        row.addView(corner)
        for (day in 1..7) {
            val date = displayedMonday.plusDays((day - 1).toLong())
            row.addView(dayHeaderCell(context, day, date, date == today))
        }
    }

    fun build(
        context: Context,
        inflater: LayoutInflater,
        container: LinearLayout,
        overlay: FrameLayout,
        courses: List<Course>,
        displayedWeek: Int,
        currentWeek: Int,
        baseMonday: LocalDate,
        onCourseClick: (Course) -> Unit = {}
    ) {
        container.removeAllViews()
        overlay.removeAllViews()
        val active = courses.filter { displayedWeek in it.startWeek..it.endWeek }
        val displayedMonday = baseMonday.plusWeeks((displayedWeek - currentWeek).toLong())
        container.addView(buildLeftColumn(context))
        for (day in 1..7) {
            val date = displayedMonday.plusDays((day - 1).toLong())
            container.addView(buildDayColumn(context, inflater, day, active, onCourseClick))
        }
        addDividerLabels(context, overlay)
    }

    private fun dp(c: Context, v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.resources.displayMetrics).toInt()

    private fun addDividerLabels(context: Context, overlay: FrameLayout) {
        val cell = dp(context, CELL_H); val dvh = dp(context, DIV_H)
        val pad = dp(context, GRID_PAD)
        overlay.addView(dividerLabel(context, "下午", pad + cell * 4))
        overlay.addView(dividerLabel(context, "晚上", pad + cell * 8 + dvh))
    }

    private fun dividerLabel(context: Context, text: String, topPx: Int): TextView {
        val tv = TextView(context)
        tv.text = text; tv.textSize = 10f; tv.gravity = Gravity.CENTER
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(context, DIV_H))
        lp.topMargin = topPx; tv.layoutParams = lp
        return tv
    }

    private fun labelCell(context: Context, text: String, hDp: Float, sizeSp: Float, bold: Boolean, bordered: Boolean = true): TextView {
        val tv = TextView(context)
        tv.text = text; tv.textSize = sizeSp; tv.gravity = Gravity.CENTER
        if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        if (bordered) tv.setBackgroundResource(R.drawable.cell_border)
        tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, hDp))
        return tv
    }

    private fun dayHeaderCell(context: Context, day: Int, date: LocalDate, isToday: Boolean): View {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.layoutParams = LinearLayout.LayoutParams(0, dp(context, HEADER_H), 1f)
        val tvDay = TextView(context)
        tvDay.text = SectionTimes.days[day - 1]; tvDay.textSize = 11f; tvDay.gravity = Gravity.CENTER
        tvDay.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        col.addView(tvDay)
        val tvDate = TextView(context)
        tvDate.text = date.dayOfMonth.toString(); tvDate.textSize = 11f; tvDate.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(dp(context, 22f), dp(context, 22f))
        lp.gravity = Gravity.CENTER_HORIZONTAL; tvDate.layoutParams = lp
        if (isToday) {
            tvDate.setBackgroundResource(R.drawable.today_circle)
            tvDate.setTextColor(ContextCompat.getColor(context, R.color.white))
            tvDate.setTypeface(tvDate.typeface, Typeface.BOLD)
        } else tvDate.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        col.addView(tvDate)
        return col
    }

    private fun dividerCell(context: Context): TextView {
        val tv = TextView(context)
        tv.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_line))
        tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, DIV_H))
        return tv
    }

    private fun dividerCount(start: Int, end: Int): Int {
        var n = 0; for (b in dividers) if (start <= b && end >= b + 1) n++; return n
    }

    private fun buildLeftColumn(context: Context): View {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(dp(context, LEFT_W), LinearLayout.LayoutParams.WRAP_CONTENT)
        for (i in 0 until SectionTimes.total) {
            val text = "第${i + 1}节\n" + SectionTimes.times[i]
            col.addView(labelCell(context, text, CELL_H, 7f, false))
            if (i + 1 == 4 || i + 1 == 8) col.addView(dividerCell(context))
        }
        return col
    }

    private fun buildDayColumn(context: Context, inflater: LayoutInflater, day: Int, active: List<Course>, onCourseClick: (Course) -> Unit): View {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        var section = 1
        while (section <= SectionTimes.total) {
            val course = active.firstOrNull { it.dayOfWeek == day && it.startSection == section }
            val lastEnd: Int
            if (course != null) {
                val end = course.endSection.coerceAtLeast(section)
                val extra = dividerCount(section, end)
                val card = inflater.inflate(R.layout.item_course_cell, col, false)
                card.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(context, CELL_H * (end - section + 1) + DIV_H * extra)
                )
                card.findViewById<View>(R.id.colorBar).setBackgroundColor(course.color)
                card.findViewById<TextView>(R.id.tvName).text = course.name
                card.findViewById<TextView>(R.id.tvRoom).text = course.classroom
                card.setPadding(dp(context, 2f), 0, 0, 0)
                card.isClickable = true
                card.setOnClickListener { onCourseClick(course) }
                col.addView(card)
                lastEnd = end
            } else {
                col.addView(labelCell(context, "", CELL_H, 10f, false))
                lastEnd = section
            }
            if (lastEnd == 4 || lastEnd == 8) col.addView(dividerCell(context))
            section = lastEnd + 1
        }
        return col
    }
}