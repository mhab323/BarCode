package com.example.barcode.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcode.data.Event
import com.example.barcode.databinding.ActivityEventDetailsBinding

@Suppress("DEPRECATION")
class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val event = intent.getParcelableExtra<Event>("EXTRA_EVENT")

        if (event != null) {
            populateEventDetails(event)
            setupCocktailRecyclerView(event)
            setUpListeners(event)
            checkEventStart(event)
        }

    }

    private fun checkEventStart(event: Event) {

        val currentTime = System.currentTimeMillis()

        if (currentTime >= event.scheduledTimeMillis && event.status == "upcoming") {

            com.example.barcode.firebase.FirebaseManager.db.collection("events")
                .document(event.eventId)
                .update("status", "live")
                .addOnSuccessListener {
                    event.status = "live"

                    android.widget.Toast.makeText(
                        this,
                        "Event is now LIVE!",
                        Toast.LENGTH_LONG
                    ).show()
                    showQRCodeDialog(event.eventId)
                }
        } else if (event.status == "live") {
            binding.btnStartLiveEvent.text = "VIEW QR CODE"
        }
    }


    private fun setUpListeners(event: Event) {
        binding.btnBack.setOnClickListener { navigateBack() }
        binding.btnStartLiveEvent.setOnClickListener {
            showQRCodeDialog(event.eventId)
        }
    }



    private fun populateEventDetails(event: Event) {
        binding.tvDetailEventName.text = event.eventName

        val infoText = "📍 ${event.location}\n" +
                "🕒 ${event.dateString}\n" +
                "👥 ${event.numOfGuests} Guests\n" +
                "💳 ${event.billingType}"

        binding.tvDetailInfo.text = infoText
    }

    private fun setupCocktailRecyclerView(event: Event) {
        val adapter = CocktailMenuAdapter(event.menu) { clickedCocktail ->
          Toast.makeText(
                this,
                "You clicked ${clickedCocktail.name}",
                Toast.LENGTH_SHORT
            ).show()

        }
        binding.rvCocktailMenu.layoutManager = LinearLayoutManager(this)
        binding.rvCocktailMenu.adapter = adapter

    }

    private fun showQRCodeDialog(eventId: String) {
        try {
            val barcodeEncoder = com.journeyapps.barcodescanner.BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(
                eventId,
                com.google.zxing.BarcodeFormat.QR_CODE,
                800,
                800
            )

            val imageView = android.widget.ImageView(this)
            imageView.setImageBitmap(bitmap)
            imageView.setPadding(32, 32, 32, 32)

            AlertDialog.Builder(this)
                .setTitle("Live Event Active!")
                .setMessage("Print this or share it so guests can order!")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Share QR Code") { _, _ ->
                    shareQRCode(bitmap, eventId)
                }
                .show()
        } catch (e: Exception) {
           Toast.makeText(
                this,
                "Failed to generate QR: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateBack() {
        finish()
    }

    private fun shareQRCode(bitmap: android.graphics.Bitmap, eventId: String) {
        try {
            val cachePath = java.io.File(cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "Event_QR_$eventId.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/png"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Scan this QR code to view our Bar Menu!"
            )
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error sharing: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}