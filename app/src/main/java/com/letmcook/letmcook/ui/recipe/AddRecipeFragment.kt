package com.letmcook.letmcook.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.letmcook.letmcook.databinding.FragmentAddRecipeBinding
import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.RecipeIngredientModel
import com.letmcook.letmcook.models.RecipeModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class AddRecipeFragment : Fragment() {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private var allIngredients: List<IngredientModel> = emptyList()
    private val addedIngredients = mutableListOf<Pair<IngredientModel, Double>>()
    private lateinit var ingredientAdapter: AddedIngredientAdapter
    private var selectedImageUri: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            binding.ivRecipeImage.setImageURI(it)
            binding.ivRecipeImage.imageTintList = null
            binding.llAddPhotoHint.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        loadIngredients()
        setupButtons()
        
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        ingredientAdapter = AddedIngredientAdapter(addedIngredients) { position ->
            addedIngredients.removeAt(position)
            ingredientAdapter.notifyItemRemoved(position)
            ingredientAdapter.notifyItemRangeChanged(position, addedIngredients.size)
        }
        binding.rvAddedIngredients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAddedIngredients.adapter = ingredientAdapter
    }

    private fun loadIngredients() {
        allIngredients = databaseService.getAllIngredients()
        val ingredientNames = allIngredients.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ingredientNames)
        binding.spinnerIngredients.setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.cardRecipeImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnAddIngredientToList.setOnClickListener {
            val selectedName = binding.spinnerIngredients.text.toString()
            val ingredient = allIngredients.find { it.name == selectedName }

            if (ingredient == null) {
                Toast.makeText(requireContext(), "Please select a valid ingredient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = binding.etIngQuantity.text.toString().toDoubleOrNull() ?: 0.0
            if (quantity <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addedIngredients.add(ingredient to quantity)
            ingredientAdapter.notifyItemInserted(addedIngredients.size - 1)
            
            // Clear inputs
            binding.spinnerIngredients.text?.clear()
            binding.etIngQuantity.text?.clear()
        }

        binding.btnSaveRecipe.setOnClickListener {
            handleSaveRecipe()
        }
    }

    private fun handleSaveRecipe() {
        val title = binding.etRecipeTitle.text.toString()
        val rawInstructions = binding.etInstructions.text.toString()
        val userId = sessionManager.getUserId() ?: "default_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (addedIngredients.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
            return
        }

        // Format instructions into a numbered list
        val lines = rawInstructions.lines().filter { it.isNotBlank() }
        val formattedInstructions = lines.mapIndexed { index, line ->
            if (line.trim().firstOrNull()?.isDigit() == true) line else "${index + 1}. $line"
        }.joinToString("\n")

        val recipeId = UUID.randomUUID().toString()
        
        // Calculate totals based on ingredients
        var totalCals = 0.0
        var totalPro = 0.0
        var totalCarb = 0.0
        var totalFat = 0.0

        val recipeIngredients = addedIngredients.map { (ing, qty) ->
            // Assuming calories/macros are per 'unit' or per 100g. 
            // Simplified: treat as per unit/g directly for now.
            totalCals += ing.calories * qty
            totalPro += ing.protein * qty
            totalCarb += ing.carbs * qty
            totalFat += ing.fat * qty

            RecipeIngredientModel(
                id = UUID.randomUUID().toString(),
                ownerId = userId,
                recipeId = recipeId,
                ingredientId = ing.id,
                requiredQuantity = qty,
                createdAt = timestamp
            )
        }

        val recipe = RecipeModel(
            id = recipeId,
            ownerId = userId,
            title = title,
            instructions = formattedInstructions,
            totalCalories = totalCals,
            totalProtein = totalPro,
            totalCarbs = totalCarb,
            totalFat = totalFat,
            imageUrl = selectedImageUri,
            createdAt = timestamp
        )

        try {
            databaseService.upsertRecipeWithIngredients(recipe, recipeIngredients)
            Toast.makeText(requireContext(), "Recipe created successfully!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
