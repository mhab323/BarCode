package com.example.barcode.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.barcode.R
import com.example.barcode.data.Event
import com.example.barcode.data.Order
import com.example.barcode.databinding.FragmentInsightsBinding
import com.example.barcode.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val rtdb = FirebaseDatabase.getInstance("https://barcode-app-71522-default-rtdb.europe-west1.firebasedatabase.app").reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEventsCount.text = "..."
        binding.tvDrinksCount.text = "..."
        binding.tvBestCocktailName.text = "Calculating..."

        loadInsights()
    }

    private fun loadInsights() {
        val currentUser = UserManager.currentUser ?: return
        val uid = auth.currentUser?.uid ?: return

        val cal = Calendar.getInstance()

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfThisMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, -1)
        val startOfLastMonth = cal.timeInMillis

        val query = if (currentUser.role == "admin") {
            db.collection("events").whereArrayContains("bartenderIds", uid)
        } else {
            db.collection("events").whereEqualTo("hostId", uid)
        }

        query.get().addOnSuccessListener { documents ->
            var eventsThisMonth = 0
            var eventsLastMonth = 0
            val eventIdsThisMonth = mutableListOf<String>()

            for (doc in documents) {
                val event = doc.toObject(Event::class.java)
                val eventTime = event.timestamp

                if (eventTime >= startOfThisMonth) {
                    eventsThisMonth++
                    eventIdsThisMonth.add(doc.id)
                } else if (eventTime in startOfLastMonth until startOfThisMonth) {
                    eventsLastMonth++
                }
            }

            binding.tvEventsCount.text = eventsThisMonth.toString()
            val eventTrend = calculateTrend(eventsThisMonth, eventsLastMonth)
            binding.tvEventsTrend.text = eventTrend
            setColorForTrend(binding.tvEventsTrend, eventsThisMonth, eventsLastMonth)

            fetchDrinkStatsForEvents(eventIdsThisMonth)

        }.addOnFailureListener { e ->
            Log.e("Insights", "Failed to load events", e)
            binding.tvEventsCount.text = "Error"
        }
    }

    private fun fetchDrinkStatsForEvents(eventIds: List<String>) {
        if (eventIds.isEmpty()) {
            binding.tvDrinksCount.text = "0"
            binding.tvDrinksTrend.text = "No events this month"
            binding.tvBestCocktailName.text = "None yet"
            binding.tvBestCocktailSales.text = "0 sold"
            return
        }

        var totalDrinks = 0
        val cocktailCounts = mutableMapOf<String, Int>()
        var processedEvents = 0

        for (eventId in eventIds) {
            rtdb.child("orders").child(eventId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order != null && order.status == "completed") {
                            totalDrinks++
                            val currentCount = cocktailCounts.getOrDefault(order.cocktailName, 0)
                            cocktailCounts[order.cocktailName] = currentCount + 1
                        }
                    }

                    processedEvents++

                    if (processedEvents == eventIds.size) {
                        updateDrinkUI(totalDrinks, cocktailCounts)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    processedEvents++
                    if (processedEvents == eventIds.size) updateDrinkUI(totalDrinks, cocktailCounts)
                }
            })
        }
    }

    private fun updateDrinkUI(totalDrinks: Int, cocktailCounts: Map<String, Int>) {
        binding.tvDrinksCount.text = totalDrinks.toString()
        binding.tvDrinksTrend.text = "Drinks poured this month"

        binding.llSalesBreakdown.removeAllViews()

        if (cocktailCounts.isNotEmpty()) {
            val bestCocktail = cocktailCounts.maxByOrNull { it.value }
            binding.tvBestCocktailName.text = bestCocktail?.key ?: "Unknown"
            binding.tvBestCocktailSales.text = "${bestCocktail?.value} sold this month"

            val sortedCocktails = cocktailCounts.toList().sortedByDescending { (_, count) -> count }

            for ((cocktailName, count) in sortedCocktails) {
                val row = android.widget.LinearLayout(requireContext())
                row.orientation = android.widget.LinearLayout.HORIZONTAL
                row.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                row.setPadding(0, 12, 0, 12)

                val nameText = android.widget.TextView(requireContext())
                nameText.text = cocktailName
                nameText.setTextColor(android.graphics.Color.parseColor("#757575"))
                nameText.textSize = 16f
                nameText.layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                val countText = android.widget.TextView(requireContext())
                countText.text = count.toString()
                countText.setTextColor(android.graphics.Color.parseColor("#3B205E"))
                countText.textSize = 16f
                countText.setTypeface(null, android.graphics.Typeface.BOLD)

                row.addView(nameText)
                row.addView(countText)
                binding.llSalesBreakdown.addView(row)
            }

        } else {
            binding.tvBestCocktailName.text = "No drinks yet"
            binding.tvBestCocktailSales.text = "0 sold"

            val emptyText = android.widget.TextView(requireContext())
            emptyText.text = "No sales data to display for this month."
            emptyText.setTextColor(android.graphics.Color.GRAY)
            binding.llSalesBreakdown.addView(emptyText)
        }
    }

    private fun calculateTrend(current: Int, previous: Int): String {
        if (previous == 0 && current > 0) return "+100% vs last month"
        if (previous == 0 && current == 0) return "0% vs last month"

        val diff = current - previous
        val percent = (diff.toDouble() / previous.toDouble()) * 100
        val formattedPercent = String.format("%.0f", percent)

        return if (diff >= 0) "+$formattedPercent% vs last month" else "$formattedPercent% vs last month"
    }

    private fun setColorForTrend(textView: android.widget.TextView, current: Int, previous: Int) {
        if (current >= previous) {
            textView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            textView.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}