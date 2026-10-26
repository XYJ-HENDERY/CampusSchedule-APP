package com.example.campusschedule.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.campusschedule.MainActivity
import com.example.campusschedule.R
import com.example.campusschedule.databinding.FragmentLoginBinding
import com.example.campusschedule.util.SessionManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val vm: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogin.setOnClickListener {
            val sid = binding.etStudentId.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()
            vm.login(sid, pwd).observe(viewLifecycleOwner) { result ->
                result.onSuccess { user ->
                    SessionManager.save(requireContext(), user.id, user.name, user.studentId, user.semesterStart)
                    (requireActivity() as MainActivity).showMain()
                }.onFailure { toast(it.message ?: "登录失败") }
            }
        }
        binding.tvRegister.setOnClickListener { switchTo(RegisterFragment()) }
        binding.tvForgot.setOnClickListener { switchTo(ResetPasswordFragment()) }
    }

    private fun switchTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}