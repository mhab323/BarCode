package com.example.barcode.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.barcode.R
import com.example.barcode.databinding.ActivityAdminHostBinding

class AdminHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHostBinding

    companion object {
        const  val USER_ROLE = "USER_ROLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userRole = intent.getStringExtra(USER_ROLE) ?: "ADMIN"

        if (userRole == "HOST") {
            setupHostExperience(savedInstanceState)
        } else {
            setupAdminExperience(savedInstanceState)
        }
    }

    private fun setupHostExperience(savedInstanceState: Bundle?) {
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.menu_host_nav)
        if (savedInstanceState == null) {
            replaceFragment(HostDashBoardFragment())
        }
        setUpHostMenu()

    }

    private fun setUpHostMenu() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_host_events -> {
                    replaceFragment(HostDashBoardFragment())
                    true
                }
                R.id.nav_host_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun setupAdminExperience(savedInstanceState: Bundle?) {
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.menu_admin_nav)
        if (savedInstanceState == null) {
            replaceFragment(AdminDashBoardFragment())
        }
        setUpAdminMenu()
    }

    private fun setUpAdminMenu() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_dashboard -> {
                    replaceFragment(AdminDashBoardFragment())
                    true
                }
                R.id.nav_admin_cocktails -> {
                    replaceFragment(MenuDashboardFragment())
                    true
                }R.id.nav_admin_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}