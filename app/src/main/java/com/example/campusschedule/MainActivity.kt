package com.example.campusschedule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.example.campusschedule.databinding.ActivityMainBinding
import com.example.campusschedule.ui.classroom.ClassroomFragment
import com.example.campusschedule.ui.grade.GradeFragment
import com.example.campusschedule.ui.login.LoginFragment
import com.example.campusschedule.ui.schedule.ScheduleFragment
import com.example.campusschedule.util.SessionManager
import com.example.campusschedule.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val night = ThemeManager.isNight(this)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        if (!SessionManager.isLoggedIn(this)) {
            showFragment(LoginFragment(), bottomNavVisible = false)
        } else {
            showFragment(ScheduleFragment(), bottomNavVisible = true)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_schedule -> ScheduleFragment()
                R.id.nav_classroom -> ClassroomFragment()
                R.id.nav_grade -> GradeFragment()
                R.id.nav_profile -> com.example.campusschedule.ui.profile.ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            replaceFragment(fragment)
            true
        }
    }

    fun showLogin() {
        supportFragmentManager.popBackStack()
        showFragment(LoginFragment(), bottomNavVisible = false)
    }

    fun showMain() {
        showFragment(ScheduleFragment(), bottomNavVisible = true)
        binding.bottomNavigation.selectedItemId = R.id.nav_schedule
    }

    private fun showFragment(fragment: Fragment, bottomNavVisible: Boolean) {
        binding.bottomNavigation.visibility =
            if (bottomNavVisible) android.view.View.VISIBLE else android.view.View.GONE
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}