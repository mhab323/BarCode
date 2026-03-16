package com.example.barcode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.barcode.databinding.ActivityLoginBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.ui.AdminDashBoardFragment
import com.example.barcode.ui.AdminHostActivity
import com.example.barcode.ui.GuestMenuActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        const val EVENT_ID = "EVENT_ID"
    }

    private val qrScannerLauncher = registerForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            val scannedEventId = result.contents
            Toast.makeText(this, "Event Found!", Toast.LENGTH_SHORT).show()

             val intent = Intent(this, GuestMenuActivity::class.java)
             intent.putExtra(EVENT_ID, scannedEventId)
             startActivity(intent)
        } else {
            Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setUpListeners()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect()
        }
    }

    private fun setUpListeners() {
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.btnGuestScan.setOnClickListener {
            barcodeScanner()

        }
    }
    private fun barcodeScanner(){
        binding.btnGuestScan.text = "CONNECTING..."
        binding.btnGuestScan.isEnabled = false

        auth.signInAnonymously()
            .addOnSuccessListener {
                binding.btnGuestScan.text = "GUEST: SCAN EVENT QR"
                binding.btnGuestScan.isEnabled = true

                val options = com.journeyapps.barcodescanner.ScanOptions()
                options.setPrompt("Scan the Event QR Code")
                options.setBeepEnabled(true)
                options.setOrientationLocked(true)
                options.setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                qrScannerLauncher.launch(options)
            }
            .addOnFailureListener {
                binding.btnGuestScan.text = "GUEST: SCAN EVENT QR"
                binding.btnGuestScan.isEnabled = true
                Toast.makeText(this, "Network Error",Toast.LENGTH_LONG).show()
            }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "LOGGING IN..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    checkUserRoleAndRedirect()
                }
            }
            .addOnFailureListener { exception ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "LOG IN"
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserRoleAndRedirect() {
        FirebaseManager.fetchAndCacheCurrentUser(
            onSuccess = { cachedUser ->
                val intent = Intent(this, AdminHostActivity::class.java)

                if (cachedUser.role == "admin") {
                    intent.putExtra(AdminHostActivity.USER_ROLE, "ADMIN")
                } else {
                    intent.putExtra(AdminHostActivity.USER_ROLE, "HOST")
                }

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            },
            onFailure = { error ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "LOG IN"
                }
        )
    }
}
