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
import com.example.barcode.databinding.FragmentAdminDashBoardBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.ui.admin.EventAdapter
import com.example.barcode.utils.CalendarHelper
import java.time.LocalDate

class AdminDashBoardFragment : Fragment() {
    private var _binding: FragmentAdminDashBoardBinding? = null
    private var eventListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private lateinit var calendarHelper: CalendarHelper
    private var allEvents = listOf<Event>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupRecyclerView()
        setupCalendar()
        fetchLiveEvents()
        setupListeners()
        loadAndDisplayShareCode()
    }
    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        binding.rvAdminEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminEvents.adapter = eventAdapter
    }
    private fun updateUIWithEvents(dailyEvents: List<Event>) {
        eventAdapter.updateEvents(dailyEvents)

        val count = dailyEvents.size
        if (count == 0) {
            binding.tvEventCount.text = "0 EVENT"
        } else {
            binding.tvEventCount.text = "$count EVENTS"
        }
    }
    private fun setupListeners() {
        binding.btnAddEvent.setOnClickListener {
            val intent = Intent(requireContext(), NewEventActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupCalendar() {
        calendarHelper = CalendarHelper(
            context = requireContext(),
            calendarView = binding.calendarView,
            monthHeader = binding.tvMonthHeader,
            btnNextMonth = binding.btnNextMonth,
            btnPreviousMonth = binding.btnPreviousMonth
        ) { clickedDate ->
            filterEventsByDate(clickedDate)
        }
        calendarHelper.setup()
    }
    private fun filterEventsByDate(selectedDate: LocalDate?) {
        if (selectedDate == null) return

        val formattedMonth = String.format("%02d", selectedDate.monthValue)
        val formattedDay = String.format("%02d", selectedDate.dayOfMonth)
        val searchString = "${selectedDate.year}-$formattedMonth-$formattedDay"

        val matchingEvents = allEvents.filter { it.dateString == searchString }
        eventAdapter.updateEvents(matchingEvents)
        updateUIWithEvents(matchingEvents)
    }
    private fun fetchLiveEvents() {
        val currentUserId = FirebaseManager.auth.currentUser?.uid ?: return

       eventListener =  FirebaseManager.listenToBartenderEvents(
            bartenderId = currentUserId,
            onSuccess = { liveList ->
                if (_binding == null) return@listenToBartenderEvents
                allEvents = liveList
                val currentDate = calendarHelper.selectedDate ?: LocalDate.now()
                filterEventsByDate(currentDate)
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Failed to load events: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventListener?.remove()
        _binding = null
    }

    private fun loadAndDisplayShareCode() {
        val currentUser = com.example.barcode.utils.UserManager.currentUser

        if (currentUser?.role == "admin") {
            if (currentUser.shareCode.isEmpty()) {
                FirebaseManager.generateAndSaveShareCode(
                    onSuccess = { newCode ->
                        binding.tvShareCode.text = newCode
                        setupCopyListener(newCode)
                    },
                    onFailure = { error ->
                        Toast.makeText(requireContext(), "Code Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                binding.tvShareCode.text = currentUser.shareCode
                setupCopyListener(currentUser.shareCode)
            }
        } else {
            binding.tvShareCode.visibility = View.GONE
            binding.tvShareCodeLabel.visibility = View.GONE
        }
    }

    private fun setupCopyListener(code: String) {
        binding.tvShareCode.setOnClickListener {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Bartender Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Share Code copied!", Toast.LENGTH_SHORT).show()
        }
    }
}