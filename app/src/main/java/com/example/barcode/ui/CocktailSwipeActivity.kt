package com.example.barcode.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.barcode.databinding.ActivityCocktailSwipeBinding
import com.example.barcode.model.Cocktail

class CocktailSwipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCocktailSwipeBinding

    private val allCocktails = mutableListOf<Cocktail>()
    private val selectedMenu = ArrayList<Cocktail>()

    private var currentIndex = 0
    private val MAX_COCKTAILS = 5


    companion object{
        const val KEY_SELECTED_MENU = "SELECTED MENU"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCocktailSwipeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        fetchCocktailsFromFirebase()

        setupListeners()
    }

    private fun fetchCocktailsFromFirebase() {
        val bartenderId = intent.getStringExtra("BARTENDER_ID")

        if (bartenderId == null) {
            Toast.makeText(this, "Error: No Bartender Connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        com.example.barcode.firebase.FirebaseManager.listenToBartenderCocktails(bartenderId,
            onSuccess = { realCocktails ->
                allCocktails.clear()
                allCocktails.addAll(realCocktails)

                if (allCocktails.isEmpty()) {
                    Toast.makeText(this, "This bartender has no drinks in their menu yet!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    displayCurrentCocktail()
                }
            },
            onFailure = { error ->
                Toast.makeText(this, "Failed to load menu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        binding.btnDislike.setOnClickListener {
            moveToNextCocktail()
        }

        binding.btnLike.setOnClickListener {
            addCocktailAndMove()
        }
    }

    private fun addCocktailAndMove(){
        val currentCocktail = allCocktails[currentIndex]
        selectedMenu.add(currentCocktail)

        binding.tvSwipeCounter.text = "Selected: ${selectedMenu.size} / $MAX_COCKTAILS"

        if (selectedMenu.size >= MAX_COCKTAILS) {
            finishWithSelection()
        } else {
            moveToNextCocktail()
        }
    }

    private fun moveToNextCocktail() {
        currentIndex++
        displayCurrentCocktail()
    }

    private fun displayCurrentCocktail() {
        if (currentIndex < allCocktails.size) {
            val currentCocktail = allCocktails[currentIndex]

            binding.tvCocktailName.text = currentCocktail.name
            binding.tvCocktailDesc.text = currentCocktail.description

            Glide.with(this)
                .load(currentCocktail.imageUrl)
                .centerCrop()
                .into(binding.ivCocktailImage)
        } else {
            Toast.makeText(this, "You've viewed all available cocktails!", Toast.LENGTH_SHORT).show()
            finishWithSelection()
        }
    }

    private fun finishWithSelection() {
        val resultIntent = Intent()
        val bundle = Bundle().apply {
            putParcelableArrayList(KEY_SELECTED_MENU, selectedMenu)
        }
        resultIntent.putExtras(bundle)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}