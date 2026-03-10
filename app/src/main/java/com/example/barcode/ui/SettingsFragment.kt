package com.example.barcode.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.barcode.auth.LoginActivity
import com.example.barcode.databinding.FragmentSettingsBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.utils.UserManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
        setupListeners()
    }

    private fun loadUserProfile() {
        val currentUser = UserManager.currentUser

        if (currentUser != null) {
            binding.tvProfileName.text = currentUser.name
            binding.tvProfileEmail.text = currentUser.email
            binding.tvProfileRole.text = currentUser.role.uppercase()
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            FirebaseManager.auth.signOut()
            UserManager.clearUser()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}