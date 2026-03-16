package com.example.barcode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.barcode.R
import com.example.barcode.databinding.ActivityLoginBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.ui.AdminHostActivity
import com.example.barcode.ui.GuestMenuActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        const val EVENT_ID = "EVENT_ID"
    }

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "Event Found!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, GuestMenuActivity::class.java)
            intent.putExtra(EVENT_ID, result.contents)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        initGoogleSignIn()
        setUpListeners()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            FirebaseManager.fetchAndCacheCurrentUser(
                onSuccess = { cachedUser ->
                    if (cachedUser.role.isNotEmpty()) {
                        redirectToDashboard(cachedUser.role)
                    }
                },
                onFailure = { /* Do nothing, stay on login screen */ }
            )
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setUpListeners() {
        binding.btnLogin.setOnClickListener { loginUser() }
        binding.tvSignUp.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }
        binding.btnGuestScan.setOnClickListener { barcodeScanner() }
        binding.btnGoogleSignIn.setOnClickListener { googleSignInLauncher.launch(googleSignInClient.signInIntent) }
    }

    private fun barcodeScanner() {
        binding.btnGuestScan.text = "CONNECTING..."
        binding.btnGuestScan.isEnabled = false

        auth.signInAnonymously()
            .addOnSuccessListener {
                binding.btnGuestScan.text = "GUEST: SCAN EVENT QR"
                binding.btnGuestScan.isEnabled = true

                val options = ScanOptions().apply {
                    setPrompt("Scan the Event QR Code")
                    setBeepEnabled(true)
                    setOrientationLocked(true)
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                }
                qrScannerLauncher.launch(options)
            }
            .addOnFailureListener {
                binding.btnGuestScan.text = "GUEST: SCAN EVENT QR"
                binding.btnGuestScan.isEnabled = true
                Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show()
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
            .addOnSuccessListener {
                checkUserRoleAndRedirect()
            }
            .addOnFailureListener { exception ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "LOG IN"
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                checkUserDatabaseStatus()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Firebase Auth Failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserRoleAndRedirect() {
        FirebaseManager.fetchAndCacheCurrentUser(
            onSuccess = { cachedUser ->
                if (cachedUser.role.isEmpty()) {
                    showRoleSelectionDialog()
                } else {
                    val intent = Intent(this, AdminHostActivity::class.java)
                    if (cachedUser.role == "admin") {
                        intent.putExtra(AdminHostActivity.USER_ROLE, "ADMIN")
                    } else {
                        intent.putExtra(AdminHostActivity.USER_ROLE, "HOST")
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = {
                showRoleSelectionDialog()
            }
        )
    }

    private fun checkUserDatabaseStatus() {
        FirebaseManager.fetchAndCacheCurrentUser(
            onSuccess = { cachedUser ->
                if (cachedUser.role.isEmpty()) {
                    showRoleSelectionDialog()
                } else {
                    redirectToDashboard(cachedUser.role)
                }
            },
            onFailure = {
                showRoleSelectionDialog()
            }
        )
    }

    private fun redirectToDashboard(role: String) {
        val intent = Intent(this, AdminHostActivity::class.java)
        intent.putExtra(AdminHostActivity.USER_ROLE, if (role == "admin") "ADMIN" else "HOST")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showRoleSelectionDialog() {
        val roles = arrayOf("Event Host", "Bartender (Admin)")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Welcome! Select Your Role")
            .setCancelable(false)
            .setItems(roles) { _, which ->
                val selectedRole = if (which == 0) "host" else "admin"
                saveNewUserToFirestore(selectedRole)
            }
            .show()
    }

    private fun saveNewUserToFirestore(role: String) {
        val currentUser = auth.currentUser ?: return

        binding.btnLogin.text = "SAVING ROLE..."
        binding.btnLogin.isEnabled = false

        val userMap = hashMapOf(
            "uid" to currentUser.uid,
            "name" to (currentUser.displayName ?: "New User"),
            "email" to (currentUser.email ?: ""),
            "role" to role,
            "shareCode" to ""
        )
        db.collection("users").document(currentUser.uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                checkUserRoleAndRedirect()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnLogin.text = "LOG IN"
                binding.btnLogin.isEnabled = true
            }
    }


}