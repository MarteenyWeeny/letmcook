package com.letmcook.letmcook.ui.grocery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.letmcook.letmcook.databinding.FragmentGroceryListBinding
import com.letmcook.letmcook.R
import com.letmcook.letmcook.models.GroceryItemModel
import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.PantryItemModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class GroceryListFragment : Fragment() {

    private var _binding: FragmentGroceryListBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: GroceryAdapter
    private var allItems: List<Pair<GroceryItemModel, IngredientModel?>> = emptyList()
    private var selectedCategory: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroceryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        loadGroceryList()

        binding.btnMoveAllToPantry.setOnClickListener {
            showMoveAllConfirmation()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroceryAdapter(emptyList(), { item ->
            showUpdateQuantityDialog(item)
        }, { item ->
            showDeleteDialog(item)
        }, { item ->
            handleMoveToPantry(item)
        }, { item ->
            performMoveToPantry(item, item.quantity)
        })
        binding.rvGrocery.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGrocery.adapter = adapter
    }

    private fun handleMoveToPantry(item: GroceryItemModel) {
        val input = EditText(requireContext())
        input.setText(item.quantity.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 16, 48, 16)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.moved_to_pantry))
            .setMessage(getString(R.string.enter_amount_transfer))
            .setView(container)
            .setPositiveButton(getString(R.string.move)) { _, _ ->
                val moveAmount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (moveAmount > 0) {
                    performMoveToPantry(item, moveAmount)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performMoveToPantry(item: GroceryItemModel, moveAmount: Double) {
        val userId = sessionManager.getUserId() ?: "default_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // 1. Add to Pantry
        val pantryItems = databaseService.getPantryItems(userId)
        val existingPantry = pantryItems.find { it.ingredientId == item.ingredientId }
        if (existingPantry != null) {
            existingPantry.currentQuantity += moveAmount
            databaseService.upsertPantryItem(existingPantry)
        } else {
            val newItem = PantryItemModel(
                id = UUID.randomUUID().toString(),
                ownerId = userId,
                ingredientId = item.ingredientId,
                currentQuantity = moveAmount,
                createdAt = timestamp
            )
            databaseService.upsertPantryItem(newItem)
        }
        
        // 2. Update Grocery List
        if (moveAmount >= item.quantity) {
            databaseService.deleteGroceryItemByIngredient(userId, item.ingredientId)
        } else {
            val remaining = item.quantity - moveAmount
            databaseService.deleteGroceryItemByIngredient(userId, item.ingredientId)
            databaseService.addOrUpdateGroceryItem(userId, item.ingredientId, remaining)
        }

        Toast.makeText(requireContext(), "Moved to Pantry", Toast.LENGTH_SHORT).show()
        loadGroceryList()
    }

    private fun showMoveAllConfirmation() {
        if (allItems.isEmpty()) return
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.move_all_to_pantry))
            .setMessage(getString(R.string.move_all_confirm))
            .setPositiveButton(getString(R.string.move_all)) { _, _ ->
                allItems.forEach { (item, _) ->
                    // Logic similar to handleMoveToPantry but without toast/reload in loop
                    moveSingleItemToPantrySilently(item)
                }
                Toast.makeText(requireContext(), "All items moved to pantry", Toast.LENGTH_SHORT).show()
                loadGroceryList()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun moveSingleItemToPantrySilently(item: GroceryItemModel) {
        val userId = sessionManager.getUserId() ?: "default_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val pantryItems = databaseService.getPantryItems(userId)
        val existing = pantryItems.find { it.ingredientId == item.ingredientId }
        
        if (existing != null) {
            existing.currentQuantity += item.quantity
            databaseService.upsertPantryItem(existing)
        } else {
            val newItem = PantryItemModel(
                id = UUID.randomUUID().toString(),
                ownerId = userId,
                ingredientId = item.ingredientId,
                currentQuantity = item.quantity,
                createdAt = timestamp
            )
            databaseService.upsertPantryItem(newItem)
        }
        databaseService.deleteGroceryItemByIngredient(userId, item.ingredientId)
    }

    private fun showUpdateQuantityDialog(item: GroceryItemModel) {
        val input = EditText(requireContext())
        input.setText(item.quantity.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 16, 48, 16)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.update_quantity))
            .setView(container)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                val newQty = input.text.toString().toDoubleOrNull() ?: item.quantity
                if (newQty >= 0) {
                    val userId = sessionManager.getUserId() ?: "default_user"
                    databaseService.deleteGroceryItemByIngredient(userId, item.ingredientId)
                    databaseService.addOrUpdateGroceryItem(userId, item.ingredientId, newQty)
                    loadGroceryList()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadGroceryList() {
        val userId = sessionManager.getUserId() ?: "default_user"
        val dbItems = databaseService.getGroceryItems(userId)
        val allIngredients = databaseService.getAllIngredients().associateBy { it.id }

        // Merge duplicates for a clean display
        allItems = dbItems.groupBy { it.ingredientId }.map { (ingId, items) ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity }
            first.copy(quantity = totalQty) to allIngredients[ingId]
        }
        
        setupCategoryFilters()
        filterList()
    }

    private fun setupCategoryFilters() {
        binding.cgCategories.removeAllViews()
        
        val categories = allItems.mapNotNull { it.second?.category }.distinct().sorted()
        
        // "All" chip
        val allChip = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = getString(R.string.all)
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
                selectedCategory = if (chip.text == getString(R.string.all)) null else chip.text.toString()
                filterList()
            } else {
                selectedCategory = null
                filterList()
            }
        }
    }

    private fun filterList() {
        val filtered = if (selectedCategory == null) {
            allItems
        } else {
            allItems.filter { it.second?.category == selectedCategory }
        }
        adapter.updateData(filtered)
    }

    private fun showDeleteDialog(item: GroceryItemModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_item_title))
            .setMessage(getString(R.string.delete_item_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val userId = sessionManager.getUserId() ?: "default_user"
                databaseService.deleteGroceryItemByIngredient(userId, item.ingredientId)
                Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show()
                loadGroceryList()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
