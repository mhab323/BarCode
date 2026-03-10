package com.example.barcode.ui.com.example.barcode.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.barcode.databinding.ActivityAddCocktailBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.model.Cocktail

class AddCocktailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCocktailBinding
    private var selectedImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            com.bumptech.glide.Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivCocktailImage)
            binding.ivCameraIcon.visibility = View.GONE
            binding.tvImageHint.visibility = View.GONE
        }
    }

    companion object {
        const val EDIT_MODE = "EDIT_MODE"
    }

    private var isEditMode = false
    private var existingCocktailId = ""
    private var existingImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCocktailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        checkForEditMode()
    }

    private fun setupListeners() {
        binding.layoutImagePicker.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSaveCocktail.setOnClickListener {
            startUploadProcess()
        }
    }

    private fun startUploadProcess() {
        val name = binding.etCocktailName.text.toString().trim()
        val desc = binding.etCocktailDesc.text.toString().trim()
        val currentUserId = FirebaseManager.auth.currentUser?.uid

        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please enter a name and description", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUserId == null) return

        binding.btnSaveCocktail.isEnabled = false
        binding.btnSaveCocktail.text = "UPLOADING..."

        if (selectedImageUri != null) {
            FirebaseManager.uploadCocktailImage(
                imageUri = selectedImageUri!!,
                onSuccess = { downloadUrl ->
                    saveCocktailFinal(name, desc, downloadUrl, currentUserId)
                },
                onFailure = { e ->
                    resetSaveButton()
                }
            )
        } else {
            saveCocktailFinal(name, desc, "", currentUserId)
        }
    }

    private fun saveCocktailFinal(name: String, desc: String, newImageUrl: String, bartenderId: String) {
        val finalImageUrl = newImageUrl.ifEmpty { existingImageUrl }

        val cocktail = Cocktail(
            name = name,
            description = desc,
            imageUrl = finalImageUrl,
            bartenderId = bartenderId
        )

        if (isEditMode) {
            cocktail.cocktailId = existingCocktailId
            FirebaseManager.updateCocktail(
                cocktail = cocktail,
                onSuccess = {
                    Toast.makeText(this, "Cocktail Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { e ->
                    resetSaveButton()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            FirebaseManager.saveCocktail(
                cocktail = cocktail,
                onSuccess = {
                    Toast.makeText(this, "Cocktail Added!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { e ->
                    resetSaveButton()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun resetSaveButton() {
        binding.btnSaveCocktail.isEnabled = true
        binding.btnSaveCocktail.text = "SAVE TO MENU"
    }

    private fun checkForEditMode() {
        isEditMode = intent.getBooleanExtra(EDIT_MODE, false)

        if (isEditMode) {
            existingCocktailId = intent.getStringExtra("COCKTAIL_ID") ?: ""
            existingImageUrl = intent.getStringExtra("COCKTAIL_IMAGE") ?: ""

            binding.tvAddTitle.text = "Edit Cocktail"
            binding.btnSaveCocktail.text = "UPDATE COCKTAIL"
            binding.etCocktailName.setText(intent.getStringExtra("COCKTAIL_NAME"))
            binding.etCocktailDesc.setText(intent.getStringExtra("COCKTAIL_DESC"))

            if (existingImageUrl.isNotEmpty()) {
                com.bumptech.glide.Glide.with(this)
                    .load(existingImageUrl)
                    .centerCrop()
                    .into(binding.ivCocktailImage)

                binding.ivCameraIcon.visibility = View.GONE
                binding.tvImageHint.visibility = View.GONE
            }
        }
    }
}