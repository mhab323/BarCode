package com.example.barcode.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.barcode.databinding.ItemCocktailMenuBinding
import com.example.barcode.model.Cocktail

class CocktailMenuAdapter(private var cocktails: List<Cocktail> , private val onCocktailClick: (Cocktail) -> Unit) :
    RecyclerView.Adapter<CocktailMenuAdapter.CocktailViewHolder>() {

     class CocktailViewHolder(val binding: ItemCocktailMenuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CocktailViewHolder {
        val binding = ItemCocktailMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CocktailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CocktailViewHolder, position: Int) {
        val cocktail = cocktails[position]

        holder.binding.tvMenuCocktailName.text = cocktail.name
        holder.binding.tvMenuCocktailDesc.text = cocktail.description

        Glide.with(holder.itemView.context)
            .load(cocktail.imageUrl)
            .centerCrop()
            .into(holder.binding.ivMenuCocktailImage)

        holder.itemView.setOnClickListener {
            onCocktailClick(cocktail)
        }
    }

    fun updateCocktails(newCocktails: List<Cocktail>) {
        this.cocktails = newCocktails
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cocktails.size
}