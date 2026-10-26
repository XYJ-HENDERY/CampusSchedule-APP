package com.example.campusschedule.ui.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.databinding.FragmentAddEditCourseBinding

class AddEditCourseFragment : Fragment() {

    private var _binding: FragmentAddEditCourseBinding? = null
    private val binding get() = _binding!!
    private val vm: AddEditCourseViewModel by viewModels()

    private val palette by lazy {
        intArrayOf(
            resources.getColor(R.color.course_red, null),
            resources.getColor(R.color.course_orange, null),
            resources.getColor(R.color.course_yellow, null),
            resources.getColor(R.color.course_green, null),
            resources.getColor(R.color.course_blue, null),
            resources.getColor(R.color.course_purple, null)
        )
    }
    private var selectedColor = 4

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAddEditCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.spinnerDay.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        )
        binding.btnSave.setOnClickListener { saveCourse() }
    }

    private fun saveCourse() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入课程名", Toast.LENGTH_SHORT).show(); return
        }
        val course = Course(
            name = name,
            teacher = binding.etTeacher.text.toString().trim(),
            classroom = binding.etClassroom.text.toString().trim(),
            dayOfWeek = binding.spinnerDay.selectedItemPosition + 1,
            startSection = binding.etStart.text.toString().toIntOrNull() ?: 1,
            endSection = binding.etEnd.text.toString().toIntOrNull() ?: 1,
            startWeek = binding.etStartWeek.text.toString().toIntOrNull() ?: 1,
            endWeek = binding.etEndWeek.text.toString().toIntOrNull() ?: 18,
            color = palette.getOrElse(selectedColor) { palette[0] },
            type = "必修"
        )
        vm.save(
            course,
            onConflict = { Toast.makeText(requireContext(), R.string.conflict_detected, Toast.LENGTH_LONG).show() },
            onSuccess = {
                Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}