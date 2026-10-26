package com.example.campusschedule.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.databinding.PageScheduleWeekBinding
import java.time.LocalDate

class WeekPagerAdapter(
    private var courses: List<Course>,
    private val currentWeek: Int,
    private val minWeek: Int,
    private val maxWeek: Int,
    private val baseMonday: LocalDate,
    private val onCourseClick: (Course) -> Unit = {}
) : RecyclerView.Adapter<WeekPagerAdapter.VH>() {

    fun submit(list: List<Course>) { courses = list; notifyDataSetChanged() }

    inner class VH(val binding: PageScheduleWeekBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = maxWeek - minWeek + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = PageScheduleWeekBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        ScheduleGridBuilder.build(
            holder.itemView.context,
            LayoutInflater.from(holder.itemView.context),
            holder.binding.gridContainer,
            holder.binding.dividerOverlay,
            courses, minWeek + position, currentWeek, baseMonday,
            onCourseClick
        )
    }
}