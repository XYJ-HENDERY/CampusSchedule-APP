package com.example.campusschedule.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.databinding.ItemCourseBinding

class CourseAdapter(
    private val onClick: (Course) -> Unit,
    private val onDelete: (Course) -> Unit
) : ListAdapter<Course, CourseAdapter.VH>(DIFF) {

    companion object {
        private val DAY_NAMES = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        private val DIFF = object : DiffUtil.ItemCallback<Course>() {
            override fun areItemsTheSame(a: Course, b: Course) = a.id == b.id
            override fun areContentsTheSame(a: Course, b: Course) = a == b
        }
    }

    inner class VH(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = getItem(position)
        with(holder.binding) {
            colorBar.setBackgroundColor(course.color)
            tvName.text = course.name
            tvInfo.text = "${course.teacher}  @ ${course.classroom}"
            tvTime.text = "${DAY_NAMES.getOrElse(course.dayOfWeek - 1) { "" }} " +
                "第${course.startSection}-${course.endSection}节  第${course.startWeek}-${course.endWeek}周"
            root.setOnClickListener { onClick(course) }
            btnDelete.setOnClickListener { onDelete(course) }
        }
    }
}