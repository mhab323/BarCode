package com.example.barcode.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.barcode.R
import com.example.barcode.auth.LoginActivity
import com.example.barcode.databinding.FragmentSettingsBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.utils.UserManager
import com.google.firebase.auth.FirebaseAuth

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
        val cachedUser = UserManager.currentUser
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (cachedUser != null) {
            binding.tvProfileName.text = cachedUser.name
            binding.tvProfileEmail.text = cachedUser.email
            binding.tvProfileRole.text = cachedUser.role.uppercase()
        }

        if (firebaseUser?.photoUrl != null) {
            Glide.with(requireContext())
                .load(firebaseUser.photoUrl)
                .placeholder(R.drawable.logo_transparent)
                .error(R.drawable.logo_transparent)
                .circleCrop()
                .into(binding.ivProfilePhoto)
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.logo_transparent)
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