package com.example.campusschedule.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.databinding.FragmentProfileDetailBinding
import com.example.campusschedule.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileDetailFragment : Fragment() {

    private var _binding: FragmentProfileDetailBinding? = null
    private val binding get() = _binding!!
    private val phoneRegex = Regex("^1\\d{10}$")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvBack.setOnClickListener { parentFragmentManager.popBackStack() }

        val uid = SessionManager.uid(requireContext())
        if (uid > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = CampusApp.instance.repository.getUser(uid)
                _binding?.let { b ->
                    if (user != null) {
                        b.tvDetailName.text = user.name.ifEmpty { "—" }
                        b.tvDetailAccount.text = user.studentId.ifEmpty { "—" }
                        b.tvDetailPhone.text = maskPhone(user.phone)
                    }
                }
            }
        }

        binding.phoneRow.setOnClickListener { showBindPhoneDialog(uid) }
    }

    private fun showBindPhoneDialog(uid: Long) {
        if (uid <= 0L) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show(); return
        }
        val input = EditText(requireContext()).apply {
            hint = "请输入11位手机号"
            inputType = InputType.TYPE_CLASS_PHONE
            filters = arrayOf(android.text.InputFilter.LengthFilter(11))
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("绑定手机号")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val phone = input.text.toString().trim()
                if (!phoneRegex.matches(phone)) {
                    Toast.makeText(requireContext(), "手机号格式不正确", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    CampusApp.instance.repository.updatePhone(uid, phone)
                    _binding?.tvDetailPhone?.text = maskPhone(phone)
                    Toast.makeText(requireContext(), "绑定成功", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun maskPhone(phone: String): String =
        if (phoneRegex.matches(phone)) phone.substring(0, 3) + "****" + phone.substring(7)
        else if (phone.isEmpty()) "未绑定" else phone

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}