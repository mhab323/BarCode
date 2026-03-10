package com.example.barcode.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcode.data.Event
import com.example.barcode.databinding.FragmentHostDashboardBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.ui.admin.EventAdapter

class HostDashBoardFragment : Fragment() {

    private var _binding: FragmentHostDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private var hostEvents = listOf<Event>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHostDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        setUpRecycleView()
        setUpListeners()
        fetchLiveHostEvents()
        updateUIState()
    }

    private fun setUpListeners() {
        binding.fabCreateEvent.setOnClickListener {
            val intent = Intent(requireContext(), NewEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUpRecycleView() {
        eventAdapter = EventAdapter(hostEvents)
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
    }

    private fun updateUIState() {
        if (hostEvents.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
        }
    }

    private fun fetchLiveHostEvents() {
        val currentUserId = FirebaseManager.auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseManager.listenToHostEvents(
            hostId = currentUserId,
            onSuccess = { liveList ->
                if (_binding == null) return@listenToHostEvents
                hostEvents = liveList
                eventAdapter.updateEvents(hostEvents)
                updateUIState()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Failed to load events: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}