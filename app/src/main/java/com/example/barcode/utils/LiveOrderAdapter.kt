package com.example.barcode.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcode.data.Order
import com.example.barcode.databinding.ItemLiveOrderBinding

class LiveOrderAdapter(
    private var orders: List<Order>,
    private val onDoneClick: (Order) -> Unit
): RecyclerView.Adapter<LiveOrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(val binding: ItemLiveOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemLiveOrderBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.binding.tvOrderCocktailName.text = order.cocktailName
        holder.binding.tvOrderGuestName.text = "For: ${order.guestName}"

        if (order.guestImageUrl.isNotEmpty()) {
            holder.binding.ivGuestPhoto.setPadding(0, 0, 0, 0)
            holder.binding.ivGuestPhoto.imageTintList = null

            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(order.guestImageUrl)
                .circleCrop()
                .into(holder.binding.ivGuestPhoto)
        } else {
            holder.binding.ivGuestPhoto.setPadding(12, 12, 12, 12)
            holder.binding.ivGuestPhoto.setImageResource(android.R.drawable.ic_menu_camera)
        }

        holder.binding.btnMarkDone.setOnClickListener {
            onDoneClick(order)
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        this.orders = newOrders
        notifyDataSetChanged()
    }


}
