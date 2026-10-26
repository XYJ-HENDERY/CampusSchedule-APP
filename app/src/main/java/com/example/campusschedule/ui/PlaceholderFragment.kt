package com.example.campusschedule.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlaceholderFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        return TextView(requireContext()).apply {
            text = arguments?.getString("title") ?: ""
            gravity = Gravity.CENTER
            textSize = 20f
        }
    }

    companion object {
        fun newInstance(title: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putString("title", title) }
        }
    }
}