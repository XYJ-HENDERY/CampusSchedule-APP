package com.example.campusschedule.ui.classroom

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.R
import com.example.campusschedule.data.model.BorrowRecord
import com.example.campusschedule.databinding.FragmentBorrowApplyBinding
import com.example.campusschedule.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class BorrowApplyFragment : Fragment() {

    private var _binding: FragmentBorrowApplyBinding? = null
    private val binding get() = _binding!!

    private var auditor: String = ""
    private var remark: String = ""
    private var attachmentPath: String = ""

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
            if (bmp != null) saveBitmapAndShow(bmp)
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) copyUriToLocal(uri)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBorrowApplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val building = args.getString(ARG_BUILDING)!!
        val room = args.getString(ARG_ROOM)!!
        val capacity = args.getInt(ARG_CAPACITY)
        val date = LocalDate.ofEpochDay(args.getLong(ARG_EPOCH_DAY))
        val s1 = args.getInt(ARG_START)
        val s2 = args.getInt(ARG_END)

        binding.tvApplyRoom.text = "$building$room"
        binding.tvApplyCapacity.text = getString(R.string.classroom_capacity, capacity)
        binding.tvApplyTime.text = getString(R.string.borrow_time_summary, date.toString(), s1, s2)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.rowRemark.setOnClickListener {
            val input = EditText(requireContext()).apply { hint = getString(R.string.borrow_remark_hint) }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.borrow_remark_label)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    remark = input.text.toString().trim()
                    binding.tvRemark.text = remark
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.btnUpload.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.borrow_attachment_label)
                .setItems(arrayOf(getString(R.string.borrow_from_camera), getString(R.string.borrow_from_gallery))) { _, which ->
                    if (which == 0) takePhotoLauncher.launch(null)
                    else pickImageLauncher.launch("image/*")
                }
                .show()
        }

        binding.btnSubmit.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length != 11) {
                Toast.makeText(requireContext(), R.string.borrow_phone_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.etAuditor.text.toString().trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.borrow_auditor_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.etSuccessor.text.toString().trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.borrow_successor_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submit(building, room, capacity, date, s1, s2, phone)
        }
    }

    private fun submit(
        building: String, room: String, capacity: Int,
        date: LocalDate, s1: Int, s2: Int, phone: String
    ) {
        val week = weekOf(date)
        val record = BorrowRecord(
            building = building, room = room, capacity = capacity,
            week = week, dayOfWeek = date.dayOfWeek.value,
            startSection = s1, endSection = s2,
            phone = phone, auditor = binding.etAuditor.text.toString().trim(),
            successor = binding.etSuccessor.text.toString().trim(),
            remark = remark,
            status = getString(R.string.borrow_status_approved),
            attachment = attachmentPath
        )
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { CampusApp.instance.repository.submitBorrow(record) }
            Toast.makeText(requireContext(), R.string.borrow_submit_success, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun weekOf(date: LocalDate): Int {
        val startMillis = SessionManager.semesterStartMillis(requireContext())
        if (startMillis <= 0L) return 1
        val start = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(start, date)
        return if (days < 0) 1 else days.toInt() / 7 + 1
    }

    private fun attachmentFile(): File {
        val dir = File(requireContext().filesDir, "attachments").apply { mkdirs() }
        return File(dir, "attach_${System.currentTimeMillis()}.jpg")
    }

    private fun saveBitmapAndShow(bmp: Bitmap) {
        runCatching {
            val file = attachmentFile()
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            attachmentPath = file.absolutePath
            binding.btnUpload.setImageBitmap(bmp)
        }.onFailure { toastFail() }
    }

    private fun copyUriToLocal(uri: Uri) {
        runCatching {
            val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("read fail")
            val file = attachmentFile()
            file.writeBytes(bytes)
            attachmentPath = file.absolutePath
            binding.btnUpload.setImageURI(Uri.fromFile(file))
        }.onFailure { toastFail() }
    }

    private fun toastFail() {
        Toast.makeText(requireContext(), R.string.borrow_attachment_failed, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BUILDING = "building"
        private const val ARG_ROOM = "room"
        private const val ARG_CAPACITY = "capacity"
        private const val ARG_EPOCH_DAY = "epochDay"
        private const val ARG_START = "startSection"
        private const val ARG_END = "endSection"

        fun newInstance(
            building: String, room: String, capacity: Int,
            epochDay: Long, startSection: Int, endSection: Int
        ) = BorrowApplyFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_BUILDING, building)
                putString(ARG_ROOM, room)
                putInt(ARG_CAPACITY, capacity)
                putLong(ARG_EPOCH_DAY, epochDay)
                putInt(ARG_START, startSection)
                putInt(ARG_END, endSection)
            }
        }
    }
}