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
import com.example.barcode.databinding.FragmentMenuDashboardBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.model.Cocktail
import androidx.appcompat.app.AlertDialog
import com.example.barcode.databinding.DialogCocktailPreviewBinding


class MenuDashboardFragment : Fragment() {

    private var _binding: FragmentMenuDashboardBinding? = null
    private val binding get() = _binding!!
    private var cocktailListener: com.google.firebase.firestore.ListenerRegistration? = null

    private lateinit var cocktailAdapter: CocktailMenuAdapter
    private var myCocktails = listOf<Cocktail>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMenuDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupRecyclerView()
        setupListeners()
        fetchMyCocktails()
    }

    private fun setupRecyclerView() {
        cocktailAdapter = CocktailMenuAdapter(myCocktails) { clickedCocktail ->
            showCocktailPreviewDialog(clickedCocktail)
        }
        binding.rvCocktails.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCocktails.adapter = cocktailAdapter
    }

    private fun setupListeners() {
        binding.btnAddCocktail.setOnClickListener {
            val intent = Intent(requireContext(), AddCocktailActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showCocktailPreviewDialog(cocktail: Cocktail) {
        val dialogBinding = DialogCocktailPreviewBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        loadPreviewData(dialogBinding, cocktail)
        setupButtons(dialogBinding, cocktail, dialog)

        dialog.show()
    }

    private fun loadPreviewData(binding: DialogCocktailPreviewBinding, cocktail: Cocktail) {
        binding.tvPreviewName.text = cocktail.name
        binding.tvPreviewDesc.text = cocktail.description

        if (cocktail.imageUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(requireContext())
                .load(cocktail.imageUrl)
                .centerCrop()
                .into(binding.ivPreviewImage)
        }
    }

    private fun setupButtons(binding: DialogCocktailPreviewBinding, cocktail: Cocktail, parentDialog: AlertDialog) {

        binding.btnEditCocktail.setOnClickListener {
            parentDialog.dismiss()

            val bundle = Bundle().apply {
                putBoolean(AddCocktailActivity.EDIT_MODE, true)
                putString("COCKTAIL_ID", cocktail.cocktailId)
                putString("COCKTAIL_NAME", cocktail.name)
                putString("COCKTAIL_DESC", cocktail.description)
                putString("COCKTAIL_IMAGE", cocktail.imageUrl)
            }

            val intent = Intent(requireContext(), AddCocktailActivity::class.java).apply {
                putExtras(bundle)
            }
            startActivity(intent)
        }
        binding.btnDeleteCocktail.setOnClickListener {
            showDeleteConfirmationDialog(cocktail, parentDialog)
        }
    }

    private fun showDeleteConfirmationDialog(cocktail: Cocktail, parentDialog: AlertDialog) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Cocktail")
            .setMessage("Are you sure you want to delete ${cocktail.name}?")
            .setPositiveButton("Yes") { _, _ ->
                executeDatabaseDeletion(cocktail, parentDialog)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeDatabaseDeletion(cocktail: Cocktail, parentDialog: AlertDialog) {
        FirebaseManager.deleteCocktail(
            cocktailId = cocktail.cocktailId,
            onSuccess = {
                Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
                parentDialog.dismiss()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun fetchMyCocktails() {
        val currentUserId = FirebaseManager.auth.currentUser?.uid
        if (currentUserId == null) return

        FirebaseManager.listenToBartenderCocktails(
            bartenderId = currentUserId,
            onSuccess = { drinks ->
                if (_binding == null) return@listenToBartenderCocktails

                myCocktails = drinks
                cocktailAdapter.updateCocktails(myCocktails)
                updateUIState()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Failed to load menu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateUIState() {
        if (myCocktails.isEmpty()) {
            binding.tvEmptyTitle.visibility = View.VISIBLE
            binding.tvEmptySubtitle.visibility = View.VISIBLE
            binding.rvCocktails.visibility = View.GONE
        } else {
            binding.tvEmptyTitle.visibility = View.GONE
            binding.tvEmptySubtitle.visibility = View.GONE
            binding.rvCocktails.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cocktailListener?.remove()
        _binding = null
    }
}