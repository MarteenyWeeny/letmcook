package com.letmcook.letmcook.ui.pantry

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.google.android.material.chip.Chip
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.FragmentPantryBinding
import com.letmcook.letmcook.databinding.DialogAddPantryItemBinding
import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.PantryItemModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class PantryFragment : Fragment() {

    private var _binding: FragmentPantryBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: PantryAdapter
    private var allPantryItems: List<Pair<PantryItemModel, IngredientModel>> = emptyList()
    private var selectedCategory: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        loadPantry()

        binding.fabAddIngredient.setOnClickListener {
            showAddIngredientDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = PantryAdapter(emptyList()) { item ->
            // Handle edit/delete
        }
        binding.rvPantry.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPantry.adapter = adapter
    }

    private fun loadPantry() {
        val userId = sessionManager.getUserId() ?: "default_user"
        val items = databaseService.getPantryItems(userId)
        val ingredients = databaseService.getAllIngredients().associateBy { it.id }
        
        allPantryItems = items.mapNotNull { item ->
            ingredients[item.ingredientId]?.let { item to it }
        }
        
        setupCategoryFilters()
        filterList()
    }

    private fun setupCategoryFilters() {
        binding.cgCategories.removeAllViews()
        
        val categories = allPantryItems.mapNotNull { it.second.category }.distinct().sorted()
        
        // "All" chip
        val allChip = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "All"
            isCheckable = true
            isChecked = selectedCategory == null
        }
        binding.cgCategories.addView(allChip)

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = category
                isCheckable = true
                isChecked = selectedCategory == category
            }
            binding.cgCategories.addView(chip)
        }

        binding.cgCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId != null) {
                val chip = group.findViewById<Chip>(checkedId)
                selectedCategory = if (chip.text == "All") null else chip.text.toString()
                filterList()
            } else {
                selectedCategory = null
                filterList()
            }
        }
    }

    private fun filterList() {
        val filtered = if (selectedCategory == null) {
            allPantryItems
        } else {
            allPantryItems.filter { it.second.category == selectedCategory }
        }
        adapter.updateData(filtered)
    }

    private fun showAddIngredientDialog() {
        val ingredients = databaseService.getAllIngredients()
        val names = ingredients.map { it.name }

        val dialogBinding = DialogAddPantryItemBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        dialogBinding.spinnerIngredients.setAdapter(spinnerAdapter)

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnAdd.setOnClickListener {
            val selectedName = dialogBinding.spinnerIngredients.text.toString()
            val ingredient = ingredients.find { it.name == selectedName }
            val quantity = dialogBinding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0

            if (ingredient != null && quantity > 0) {
                val userId = sessionManager.getUserId() ?: "default_user"
                val newItem = PantryItemModel(
                    id = UUID.randomUUID().toString(),
                    ownerId = userId,
                    ingredientId = ingredient.id,
                    currentQuantity = quantity,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                databaseService.upsertPantryItem(newItem)
                loadPantry()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
