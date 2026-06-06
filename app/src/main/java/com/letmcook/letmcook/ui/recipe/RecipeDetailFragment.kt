package com.letmcook.letmcook.ui.recipe

import android.os.Bundle
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.FragmentRecipeDetailBinding
import com.letmcook.letmcook.models.IntakeModel
import com.letmcook.letmcook.models.RecipeModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private var recipe: RecipeModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        val recipeId = arguments?.getString("recipeId")
        if (recipeId != null) {
            loadRecipe(recipeId)
        }

        binding.btnCookedThis.setOnClickListener {
            handleCookedThis()
        }

        binding.btnAddMissing.setOnClickListener {
            handleAddMissing()
        }

        binding.btnDeleteRecipe.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showDeleteConfirmation() {
        val r = recipe ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete '${r.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                databaseService.deleteRecipe(r.id)
                Toast.makeText(requireContext(), "Recipe deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadRecipe(id: String) {
        val allRecipes = databaseService.getAllRecipes()
        recipe = allRecipes.find { it.id == id }
        
        recipe?.let { r ->
            binding.tvRecipeTitle.text = r.title
            binding.tvMacros.text = "${r.totalCalories.toInt()} kcal | P: ${r.totalProtein.toInt()}g | C: ${r.totalCarbs.toInt()}g | F: ${r.totalFat.toInt()}g"
            binding.tvInstructions.text = r.instructions

            if (r.imageUrl != null) {
                binding.ivRecipeImage.setImageURI(android.net.Uri.parse(r.imageUrl))
                binding.ivRecipeImage.imageTintList = null
            } else {
                binding.ivRecipeImage.setImageResource(android.R.drawable.ic_menu_gallery)
                binding.ivRecipeImage.imageTintList = android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.slate_light)
                )
            }

            val userId = sessionManager.getUserId() ?: "default_user"
            val pantryItems = databaseService.getPantryItems(userId).associateBy { it.ingredientId }
            val ingredients = databaseService.getRecipeIngredients(r.id)
            val allIngredients = databaseService.getAllIngredients().associateBy { it.id }

            val ingListText = StringBuilder()
            ingredients.forEach { ri ->
                val ing = allIngredients[ri.ingredientId]
                val name = ing?.name ?: "Unknown"
                val pi = pantryItems[ri.ingredientId]
                val hasEnough = pi != null && pi.currentQuantity >= ri.requiredQuantity
                val status = if (hasEnough) "[In Pantry]" else "[Missing]"
                ingListText.append("• $name (${ri.requiredQuantity}${ing?.unitOfMeasure ?: ""}) $status\n")
            }
            binding.tvIngredientsList.text = ingListText.toString()
        }
    }

    private fun handleCookedThis() {
        val r = recipe ?: return
        val userId = sessionManager.getUserId() ?: "default_user"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val intake = IntakeModel(
            id = UUID.randomUUID().toString(),
            ownerId = userId,
            recipeId = r.id,
            quantity = 1.0,
            date = today,
            calories = r.totalCalories,
            protein = r.totalProtein,
            carbs = r.totalCarbs,
            fat = r.totalFat,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        databaseService.upsertIntake(intake)

        // Decrement pantry items
        val ingredients = databaseService.getRecipeIngredients(r.id)
        val pantryItems = databaseService.getPantryItems(userId).associateBy { it.ingredientId }

        ingredients.forEach { ri ->
            pantryItems[ri.ingredientId]?.let { pi ->
                val newQty = (pi.currentQuantity - ri.requiredQuantity).coerceAtLeast(0.0)
                pi.currentQuantity = newQty
                databaseService.upsertPantryItem(pi)
            }
        }

        Toast.makeText(requireContext(), "Meal logged! Pantry updated.", Toast.LENGTH_SHORT).show()
        
        // Refresh UI to show updated pantry status
        loadRecipe(r.id)
    }

    private fun handleAddMissing() {
        val r = recipe ?: return
        val userId = sessionManager.getUserId() ?: "default_user"
        val ingredients = databaseService.getRecipeIngredients(r.id)

        ingredients.forEach { ri ->
            databaseService.addOrUpdateGroceryItem(userId, ri.ingredientId, ri.requiredQuantity)
        }

        Toast.makeText(requireContext(), "All ingredients added to grocery list.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
