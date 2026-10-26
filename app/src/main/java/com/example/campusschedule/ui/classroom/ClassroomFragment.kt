package com.example.campusschedule.ui.classroom

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Classroom
import com.example.campusschedule.databinding.FragmentClassroomBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ClassroomFragment : Fragment() {

    private var _binding: FragmentClassroomBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClassroomViewModel by viewModels()
    private lateinit var adapter: ClassroomAdapter
    private lateinit var recordAdapter: BorrowRecordAdapter
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedPair: Int = 1

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentClassroomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ClassroomAdapter { room -> openApplyPage(room) }
        binding.recyclerClassrooms.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerClassrooms.adapter = adapter

        recordAdapter = BorrowRecordAdapter()
        binding.recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecords.adapter = recordAdapter

        updateDateLabel()
        binding.tvDate.setOnClickListener { showDatePicker() }

        val sectionAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.classroom_sections, android.R.layout.simple_spinner_item
        )
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSection.adapter = sectionAdapter
        binding.spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedPair = pos + 1
                viewModel.setSectionPair(pos + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.tabQuery.setOnClickListener { showPage(query = true) }
        binding.tabRecords.setOnClickListener { showPage(query = false) }

        viewModel.results.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.records.observe(viewLifecycleOwner) { list ->
            recordAdapter.submitList(list)
            binding.tvRecordEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showPage(query: Boolean) {
        binding.layoutQuery.visibility = if (query) View.VISIBLE else View.GONE
        binding.layoutRecords.visibility = if (query) View.GONE else View.VISIBLE
        val active = resources.getColor(R.color.primary, null)
        val inactive = resources.getColor(R.color.text_primary, null)
        binding.tvTabQuery.setTextColor(if (query) active else inactive)
        binding.tvTabRecords.setTextColor(if (query) inactive else active)
    }

    private fun openApplyPage(room: Classroom) {
        val s1 = selectedPair * 2 - 1
        val s2 = selectedPair * 2
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer,
                BorrowApplyFragment.newInstance(room.building, room.room, room.capacity,
                    selectedDate.toEpochDay(), s1, s2))
            .addToBackStack(null)
            .commit()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                updateDateLabel()
                viewModel.setDate(selectedDate)
            },
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
        ).show()
    }

    private fun updateDateLabel() {
        binding.tvDate.text = getString(R.string.classroom_time, selectedDate.format(formatter))
    }

    private fun onBorrow(room: Classroom) {
        Toast.makeText(
            requireContext(),
            getString(R.string.classroom_borrow_toast, "${room.building}${room.room}"),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}