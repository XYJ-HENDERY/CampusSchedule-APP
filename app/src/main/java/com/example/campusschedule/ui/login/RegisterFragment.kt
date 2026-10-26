package com.example.campusschedule.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.campusschedule.R
import com.example.campusschedule.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val vm: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRegister.setOnClickListener {
            vm.register(
                binding.etName.text.toString(),
                binding.etStudentId.text.toString().trim(),
                binding.etPassword.text.toString(),
                binding.etConfirm.text.toString()
            ).observe(viewLifecycleOwner) { result ->
                result.onSuccess {
                    Toast.makeText(requireContext(), "注册成功，请登录", Toast.LENGTH_SHORT).show()
                    backToLogin()
                }.onFailure { Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show() }
            }
        }
        binding.tvBackToLogin.setOnClickListener { backToLogin() }
    }

    private fun backToLogin() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}