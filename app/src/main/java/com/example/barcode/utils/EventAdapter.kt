package com.example.barcode.ui.admin


import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcode.data.Event
import com.example.barcode.databinding.ItemEventCardBinding
import com.example.barcode.ui.EventDetailsActivity

class EventAdapter(private var events: List<Event> = emptyList()) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(val binding: ItemEventCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.binding.tvCardEventName.text = event.eventName
        holder.binding.tvCardLocationTime.text = "📍${event.location} • ${event.dateString} at ${event.timeString}"
        holder.binding.tvCardBilling.text = event.billingType
        holder.binding.tvCardGuests.text = "👥 ${event.numOfGuests} Guests"
        holder.binding.tvCardMenuSize.text = "${event.menu.size} Drinks Menu"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EventDetailsActivity::class.java)
            intent.putExtra("EXTRA_EVENT", event)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = events.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}