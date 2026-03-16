package com.example.barcode.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcode.databinding.ActivityLiveBartenderBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.utils.LiveOrderAdapter

class LiveBartenderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveBartenderBinding
    private lateinit var adapter: LiveOrderAdapter
    private var currentEventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBartenderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEventId = intent.getStringExtra("EVENT_ID") ?: ""

        if (currentEventId.isEmpty()) {
            Toast.makeText(this, "Error: No Event ID found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        startListeningForOrders()
        setUpListeners()
    }

    private fun setUpListeners() {
        binding.btnEndEvent.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("End Event")
                .setMessage("Are you sure you want to end this event? This will stop all incoming orders.")
                .setPositiveButton("End Event") { _, _ ->
                    endLiveEvent()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun endLiveEvent() {
        FirebaseManager.db.collection("events").document(currentEventId)
            .update("status", "completed")
            .addOnSuccessListener {
                Toast.makeText(this, "Event Ended Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to end event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        adapter = LiveOrderAdapter(emptyList()) { clickedOrder ->
            FirebaseManager.completeOrder(currentEventId, clickedOrder.orderId)
            Toast.makeText(this, "${clickedOrder.cocktailName} ready!", Toast.LENGTH_SHORT).show()
        }
        binding.rvLiveOrders.layoutManager = LinearLayoutManager(this)
        binding.rvLiveOrders.adapter = adapter
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun startListeningForOrders() {
        FirebaseManager.listenToLiveOrders(currentEventId) { liveOrders ->
            adapter.updateOrders(liveOrders)
        }
    }
}