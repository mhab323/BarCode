package com.example.barcode.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcode.R
import com.example.barcode.auth.LoginActivity
import com.example.barcode.data.Event
import com.example.barcode.databinding.ActivityGuestMenuBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.data.Order
import com.example.barcode.utils.SoundEffectPlayer
import com.example.barcode.utils.VibrationManager

class GuestMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestMenuBinding
    private lateinit var vibrationManager: VibrationManager
    private var currentEventId: String = ""
    private var uploadedSelfieUrl: String = ""
    private lateinit var takeSelfieLauncher: androidx.activity.result.ActivityResultLauncher<Void?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSoundEffect()
        vibrationManager = VibrationManager(this)

        currentEventId = intent.getStringExtra(LoginActivity.EVENT_ID) ?: ""

        if (currentEventId.isEmpty()) {
            Toast.makeText(this, "Invalid Event QR!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadEventMenu()
        setupCameraLauncher()
        setUpListener()
    }

    private fun setUpListener() {
        binding.ivGuestSelfie.setOnClickListener {
            takeSelfieLauncher.launch(null)
        }    }

    private fun initSoundEffect() {
        SoundEffectPlayer.init(this)
        SoundEffectPlayer.load(this,R.raw.drink_ready)
    }

    private fun loadEventMenu() {
        val cleanEventId = currentEventId.trim()

        FirebaseManager.db.collection("events").document(cleanEventId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val event = document.toObject(Event::class.java)
                    if (event != null) {
                        binding.tvGuestEventName.text = event.eventName
                        setupRecyclerView(event)
                    } else {
                        Toast.makeText(this, "Failed to read event data.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Event Not Found! Scanned ID: [$cleanEventId]", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun setupRecyclerView(event: Event) {
        val adapter = CocktailMenuAdapter(event.menu) { clickedCocktail ->
            placeOrder(clickedCocktail.name)
        }
        binding.rvGuestMenu.layoutManager = LinearLayoutManager(this)
        binding.rvGuestMenu.adapter = adapter
    }

    private fun placeOrder(drinkName: String) {
        val guestName = binding.etGuestName.text.toString().trim()

        if (guestName.isEmpty()) {
            Toast.makeText(this, "Please enter your name first!", Toast.LENGTH_SHORT).show()
            return
        }

        val newOrder = Order(
            eventId = currentEventId,
            guestName = guestName,
            cocktailName = drinkName,
            timestamp = System.currentTimeMillis(),
            status = "pending",
            guestImageUrl = uploadedSelfieUrl
        )

        val orderStartTime = System.currentTimeMillis()

        FirebaseManager.placeLiveOrder(newOrder,
            onSuccess = {
                binding.layoutWaitingOverlay.visibility = android.view.View.VISIBLE

                FirebaseManager.listenToOrderStatus(newOrder.eventId, newOrder.orderId) {
                    binding.layoutWaitingOverlay.visibility = android.view.View.GONE

                    val waitTimeMillis = System.currentTimeMillis() - orderStartTime
                    val minutes = (waitTimeMillis / 1000) / 60
                    val seconds = (waitTimeMillis / 1000) % 60
                    val timeString = if (minutes > 0) "$minutes min $seconds sec" else "$seconds seconds"
                    vibrationManager.vibrate(500)
                    SoundEffectPlayer.play(R.raw.drink_ready)

                    AlertDialog.Builder(this@GuestMenuActivity)
                        .setTitle("🎉 Drink Ready!")
                        .setMessage("Hey ${newOrder.guestName}, your ${newOrder.cocktailName} is ready at the bar!\n\n⏱️ Wait time: $timeString")                        .setPositiveButton("GOT IT") { dialog, _ -> dialog.dismiss() }
                        .setCancelable(false)
                        .show()
                }            },
            onFailure = { error ->
                Toast.makeText(this, "Failed to order: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupCameraLauncher() {
        takeSelfieLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
        ) { bitmap: android.graphics.Bitmap? ->
            if (bitmap != null) {
                com.bumptech.glide.Glide.with(this).load(bitmap).circleCrop().into(binding.ivGuestSelfie)
                binding.ivGuestSelfie.setPadding(0, 0, 0, 0)
                binding.ivGuestSelfie.imageTintList = null

                binding.layoutWaitingOverlay.visibility = android.view.View.VISIBLE

                FirebaseManager.uploadGuestSelfie(bitmap,
                    onSuccess = { url ->
                        uploadedSelfieUrl = url
                        binding.layoutWaitingOverlay.visibility = android.view.View.GONE
                        Toast.makeText(this, "Selfie attached!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        binding.layoutWaitingOverlay.visibility = android.view.View.GONE
                     Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}