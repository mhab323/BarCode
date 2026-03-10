package com.example.barcode.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcode.auth.LoginActivity
import com.example.barcode.data.Event
import com.example.barcode.databinding.ActivityGuestMenuBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.data.Order

class GuestMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestMenuBinding
    private var currentEventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEventId = intent.getStringExtra(LoginActivity.EVENT_ID) ?: ""

        if (currentEventId.isEmpty()) {
            Toast.makeText(this, "Invalid Event QR!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadEventMenu()
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
            status = "pending"
        )

        FirebaseManager.placeLiveOrder(newOrder,
            onSuccess = {
                Toast.makeText(this, "Order sent to the bar! 🍸", Toast.LENGTH_LONG).show()
            },
            onFailure = { error ->
                Toast.makeText(this, "Failed to order: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}