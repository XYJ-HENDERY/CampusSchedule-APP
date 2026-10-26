package com.example.campusschedule.ui.schedule

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.campusschedule.CampusApp
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Course
import com.example.campusschedule.databinding.FragmentScheduleBinding
import com.example.campusschedule.ui.course.AddEditCourseFragment
import com.example.campusschedule.util.ScheduleImporter
import com.example.campusschedule.util.SectionTimes
import com.example.campusschedule.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private val vm: ScheduleViewModel by viewModels()

    private val rangeStart = LocalDate.of(2026, 9, 7)
    private val rangeEnd = LocalDate.of(2027, 1, 1)
    private val semesterMonday = LocalDate.of(2026, 9, 7)

    private var currentWeek = 1
    private var minWeek = 1
    private var maxWeek = 1
    private lateinit var baseMonday: LocalDate
    private lateinit var adapter: WeekPagerAdapter

    private val uiPrefs by lazy { requireContext().getSharedPreferences("ui", android.content.Context.MODE_PRIVATE) }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleImport(it) }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val today = LocalDate.now()
        baseMonday = mondayOf(today)
        currentWeek = (ChronoUnit.DAYS.between(semesterMonday, baseMonday) / 7).toInt() + 1
        minWeek = currentWeek + ChronoUnit.WEEKS.between(baseMonday, mondayOf(rangeStart)).toInt()
        maxWeek = currentWeek + ChronoUnit.WEEKS.between(baseMonday, mondayOf(rangeEnd)).toInt()

        binding.tvWeek.text = "${today.year}年${today.monthValue}月${today.dayOfMonth}日"
        binding.btnTheme.text = if (ThemeManager.isNight(requireContext())) "☀️" else "🌙"
        binding.btnTheme.setOnClickListener {
            ThemeManager.toggle(requireContext())
            requireActivity().recreate()
        }
        binding.btnToday.setOnClickListener {
            binding.viewPager.setCurrentItem(currentWeek - minWeek, true)
        }
        binding.fabAdd.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("添加课表")
                .setItems(arrayOf("手动添加课程", "从教务课表文件导入")) { _, which ->
                    if (which == 0) {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, AddEditCourseFragment())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        importLauncher.launch(arrayOf("*/*"))
                    }
                }
                .show()
        }

        adapter = WeekPagerAdapter(emptyList(), currentWeek, minWeek, maxWeek, baseMonday) { course ->
            showCourseDetail(course)
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        val savedWeek = uiPrefs.getInt("displayed_week", currentWeek).coerceIn(minWeek, maxWeek)
        binding.viewPager.setCurrentItem(savedWeek - minWeek, false)
        showWeek(savedWeek)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { showWeek(minWeek + position) }
        })

        val gd = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (abs(vx) > abs(vy) && abs(vx) > 150) {
                    val target = binding.viewPager.currentItem + (if (vx < 0) 1 else -1)
                    if (target in 0 until adapter.itemCount) binding.viewPager.setCurrentItem(target, true)
                    return true
                }
                return false
            }
        })
        binding.weekHeader.setOnTouchListener { _, e -> gd.onTouchEvent(e); true }

        vm.courses.observe(viewLifecycleOwner) { adapter.submit(it) }
    }

    private fun showWeek(week: Int) {
        uiPrefs.edit().putInt("displayed_week", week).apply()
        binding.tvWeekNum.text = "第${week}周"
        ScheduleGridBuilder.buildWeekHeader(requireContext(), binding.weekHeader, week, currentWeek, baseMonday)
    }

    private fun showCourseDetail(c: Course) {
        val start = SectionTimes.times[(c.startSection - 1).coerceIn(SectionTimes.times.indices)].substringBefore("\n")
        val end = SectionTimes.times[(c.endSection - 1).coerceIn(SectionTimes.times.indices)].substringAfter("\n")
        val msg = buildString {
            appendLine("时间：${SectionTimes.days.getOrElse(c.dayOfWeek - 1) { "" }} $start-$end")
            appendLine("地点：${c.classroom}")
            appendLine("节次：第${c.startSection}-${c.endSection}节")
            append("周数：第${c.startWeek}-${c.endWeek}周")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(c.name)
            .setMessage(msg)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun handleImport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                val bytes = requireContext().contentResolver
                    .openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取所选文件")
                ScheduleImporter.parse(bytes)
            }
            result.fold(
                onSuccess = { confirmImport(it) },
                onFailure = { err ->
                    Toast.makeText(requireContext(), err.message ?: "导入失败", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun confirmImport(list: List<Course>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("导入课表")
            .setMessage("共解析到 ${list.size} 门课程,导入后将替换当前课表,是否继续?")
            .setPositiveButton("导入") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { CampusApp.instance.repository.importCourses(list) }
                    Toast.makeText(requireContext(), "导入成功,共 ${list.size} 门课程", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun mondayOf(d: LocalDate) = d.minusDays((d.dayOfWeek.value - 1).toLong())

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}