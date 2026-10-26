package com.example.campusschedule.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.campusschedule.CampusApp
import com.example.campusschedule.MainActivity
import com.example.campusschedule.R
import com.example.campusschedule.databinding.FragmentProfileBinding
import com.example.campusschedule.util.SessionManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        binding.tvName.text = SessionManager.userName(ctx).ifEmpty { "未登录" }
        binding.tvStudentId.text = SessionManager.studentId(ctx).ifEmpty { "—" }

        val uid = SessionManager.uid(ctx)
        if (uid > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = CampusApp.instance.repository.getUser(uid)
                if (user == null) {
                    SessionManager.logout(ctx)
                    (requireActivity() as MainActivity).showLogin()
                    return@launch
                }
                _binding?.let { b ->
                    b.tvName.text = user.name.ifEmpty { "未登录" }
                    b.tvStudentId.text = user.studentId.ifEmpty { "—" }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            SessionManager.logout(ctx)
            (requireActivity() as MainActivity).showLogin()
        }

        binding.headerInfo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileDetailFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}