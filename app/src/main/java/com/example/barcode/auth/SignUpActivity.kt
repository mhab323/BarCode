package com.example.barcode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.barcode.databinding.ActivitySignupBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.ui.AdminHostActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupListeners()
    }

    private fun setupListeners() {
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.etSignUpName.text.toString().trim()
        val email = binding.etSignUpEmail.text.toString().trim()
        val password = binding.etSignUpPassword.text.toString().trim()

        val role = if (binding.rgRole.checkedRadioButtonId == binding.rbAdmin.id) {
            "admin"
        } else {
            "host"
        }

        FirebaseManager.currentUserRole = role

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            binding.etSignUpPassword.error = "Password must be at least 6 characters"
            return
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "CREATING ACCOUNT..."

        createAccount(name,email,password,role)


    }

    private fun createAccount(name :String, email :String,password :String,role :String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    saveUserToFirestore(userId, name, email, role)
                }
            }
            .addOnFailureListener { exception ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "SIGN UP"
                Toast.makeText(this, "Auth Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String, role: String) {
        val userMap = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "role" to role
        )

        db.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {

                com.example.barcode.utils.UserManager.currentUser = com.example.barcode.model.User(
                    uid = userId,
                    name = name,
                    email = email,
                    role = role,
                    shareCode = ""
                )

                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, AdminHostActivity::class.java)
                if (role == "host") {
                    intent.putExtra(AdminHostActivity.USER_ROLE, "HOST")
                } else {
                    intent.putExtra(AdminHostActivity.USER_ROLE, "ADMIN")
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "SIGN UP"
                Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}