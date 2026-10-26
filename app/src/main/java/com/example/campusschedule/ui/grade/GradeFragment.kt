package com.example.campusschedule.ui.grade

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Grade
import com.example.campusschedule.data.model.gradeTextToGpa
import com.example.campusschedule.databinding.DialogAddGradeBinding
import com.example.campusschedule.databinding.FragmentGradeBinding
import com.example.campusschedule.databinding.ItemGradeRowBinding
import com.example.campusschedule.util.GradeImporter
import com.example.campusschedule.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GradeFragment : Fragment() {

    private var _binding: FragmentGradeBinding? = null
    private val binding get() = _binding!!
    private val vm: GradeViewModel by viewModels()

    private var sortCol = ""
    private var sortAsc = false
    private var pendingSemester = ""

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) handleImport(uri)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentGradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvGradeName.text = SessionManager.userName(requireContext())
        binding.tvGradeStudentId.text =
            getString(R.string.grade_student_id_prefix, SessionManager.studentId(requireContext()))

        val options = vm.semesterOptions()
        val semAdapter = object : ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_item, options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(resources.getColor(R.color.text_primary, null))
                return v
            }
        }
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSemester.adapter = semAdapter
        binding.spSemester.setSelection(0)
        binding.spSemester.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    vm.selected.value = options[pos]
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }

        binding.btnGradeAdd.setOnClickListener { showAddDialog(options) }
        binding.spSemester.setOnLongClickListener {
            val sem = vm.selected.value
            if (sem == GradeViewModel.SEM_ALL) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("清空全部成绩")
                    .setMessage("将删除所有学期的成绩记录,不可恢复")
                    .setPositiveButton("清空") { _, _ -> vm.clearAll() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("清空 $sem 的成绩")
                    .setMessage("将删除该学期全部成绩记录,其他学期不受影响")
                    .setPositiveButton("清空") { _, _ -> vm.clearSemester(sem) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            true
        }
        binding.btnGradeImport.setOnClickListener {
            val sems = options.filter { it != GradeViewModel.SEM_ALL }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("这份成绩单是哪个学期的?")
                .setSingleChoiceItems(sems.toTypedArray(), sems.indexOf(vm.currentSemester())) { d, which ->
                    pendingSemester = sems[which]
                    d.dismiss()
                    importLauncher.launch(arrayOf("*/*"))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        vm.grades.observe(viewLifecycleOwner) { render(it) }
    }

    private fun arrow(col: String) = when {
        sortCol != col -> " ↕"
        sortAsc -> " ▲"
        else -> " ▼"
    }

    private fun toggleSort(col: String) {
        if (sortCol == col) sortAsc = !sortAsc else { sortCol = col; sortAsc = false }
        vm.grades.value?.let { render(it) }
    }

    private fun scoreSortKey(text: String): Double =
        text.trim().toDoubleOrNull() ?: when (text.trim()) {
            "优", "优秀" -> 95.0
            "良" -> 85.0
            "中" -> 75.0
            "及格" -> 65.0
            "不及格" -> 50.0
            else -> -1.0
        }

    private fun render(list: List<Grade>) {
        val header = ItemGradeRowBinding.bind(binding.gradeHeader.root)
        header.cellCourse.text = getString(R.string.grade_col_course)
        header.cellCode.text = getString(R.string.grade_col_code)
        header.cellProperty.text = getString(R.string.grade_col_property)
        header.cellCredit.text = getString(R.string.grade_col_credit) + arrow("credit")
        header.cellScore.text = getString(R.string.grade_col_score) + arrow("score")
        header.cellCredit.setOnClickListener { toggleSort("credit") }
        header.cellScore.setOnClickListener { toggleSort("score") }

        var data: List<Grade> = list
        if (sortCol.isNotEmpty()) {
            data = if (sortCol == "credit") list.sortedBy { it.credit }
            else list.sortedBy { scoreSortKey(it.gradeText) }
            if (sortAsc) data = data.reversed()
        }

        binding.llGradeRows.removeAllViews()
        data.forEach { g ->
            val row = ItemGradeRowBinding.inflate(layoutInflater, binding.llGradeRows, false)
            row.cellCourse.text = g.courseName
            row.cellCode.text = g.courseCode
            row.cellCredit.text = if (g.credit > 0) g.credit.toString() else "-"
            row.cellProperty.text = g.property.ifEmpty { "-" }
            row.cellScore.text = g.gradeText
            row.root.setOnClickListener { showChangeSemesterDialog(g) }
            binding.llGradeRows.addView(row.root)
        }
        binding.tvGradeEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE

        val totalCredit = list.sumOf { it.credit }
        val avg = if (totalCredit > 0) list.sumOf { it.credit * it.gpa } / totalCredit
        else if (list.isNotEmpty()) list.map { it.gpa }.average() else 0.0
        binding.tvGradeAvg.text = getString(R.string.grade_avg_prefix) + String.format("%.2f", avg)
    }

    private fun showAddDialog(semesterOptions: List<String>) {
        val dlg = DialogAddGradeBinding.inflate(layoutInflater)
        val props = resources.getStringArray(R.array.grade_properties)
        dlg.spDlgProperty.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, props)
        val sems = semesterOptions.filter { it != GradeViewModel.SEM_ALL }
        dlg.spDlgSemester.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sems)
        val current = vm.currentSemester()
        val defaultIdx = sems.indexOf(current).let { if (it >= 0) it else 0 }
        dlg.spDlgSemester.setSelection(defaultIdx)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.grade_add)
            .setView(dlg.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val course = dlg.etDlgCourse.text.toString().trim()
                val grade = dlg.etDlgGrade.text.toString().trim()
                if (course.isEmpty() || grade.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.grade_fields_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                vm.addGrade(
                    Grade(
                        semester = dlg.spDlgSemester.selectedItem.toString(),
                        courseName = course,
                        courseCode = dlg.etDlgCode.text.toString().trim(),
                        credit = dlg.etDlgCredit.text.toString().toDoubleOrNull() ?: 0.0,
                        property = dlg.spDlgProperty.selectedItem.toString(),
                        gradeText = grade,
                        gpa = gradeTextToGpa(grade)
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showChangeSemesterDialog(g: Grade) {
        val sems = vm.semesterOptions().filter { it != GradeViewModel.SEM_ALL }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(g.courseName)
            .setSingleChoiceItems(sems.toTypedArray(), sems.indexOf(g.semester)) { d, which ->
                vm.updateGrade(g.copy(semester = sems[which]))
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleImport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                val bytes = requireContext().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() } ?: error("empty")
                withContext(Dispatchers.IO) {
                    GradeImporter.parse(bytes, defaultSemester = pendingSemester.ifEmpty { vm.currentSemester() })
                }
            }
            result.onSuccess { list ->
                val sem = pendingSemester.ifEmpty { GradeViewModel.SEM_ALL }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.grade_report)
                    .setMessage("共解析到 ${list.size} 条成绩，将导入到「$sem」学期（仅替换该学期数据）")
                    .setPositiveButton(R.string.grade_import_ok) { _, _ ->
                        vm.importForSemester(list, sem)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }.onFailure { e ->
                Toast.makeText(
                    requireContext(),
                    e.message ?: getString(R.string.grade_import_fail),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}