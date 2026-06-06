package com.letmcook.letmcook.ui.recipe

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.FragmentRecipeExploreBinding
import com.letmcook.letmcook.models.RecipeModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager

class RecipeExploreFragment : Fragment() {

    private var _binding: FragmentRecipeExploreBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: RecipeAdapter
    private var allRecipes: List<RecipeModel> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecipeExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        loadRecipes()
        setupSearch()
        setupAddButton()
    }

    private fun setupAddButton() {
        binding.fabAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_recipeExploreFragment_to_addRecipeFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(emptyList()) { recipe ->
            val bundle = Bundle().apply {
                putString("recipeId", recipe.id)
            }
            findNavController().navigate(R.id.action_recipeExploreFragment_to_recipeDetailFragment, bundle)
        }
        binding.rvRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecipes.adapter = adapter
    }

    private fun loadRecipes() {
        val userId = sessionManager.getUserId() ?: "default_user"
        allRecipes = databaseService.getAllRecipes()
        val pantryItems = databaseService.getPantryItems(userId).associateBy { it.ingredientId }

        val scoredRecipes = allRecipes.map { recipe ->
            val ingredients = databaseService.getRecipeIngredients(recipe.id)
            val matchCount = ingredients.count { ri -> 
                val pi = pantryItems[ri.ingredientId]
                pi != null && pi.currentQuantity >= ri.requiredQuantity 
            }
            val score = if (ingredients.isEmpty()) 0.0 else matchCount.toDouble() / ingredients.size
            recipe to score
        }.sortedByDescending { it.second }

        adapter.updateData(scoredRecipes)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterRecipes(query: String) {
        val userId = sessionManager.getUserId() ?: "default_user"
        val pantryItems = databaseService.getPantryItems(userId).associateBy { it.ingredientId }

        val filtered = allRecipes.filter { it.title.contains(query, ignoreCase = true) }
        val scored = filtered.map { recipe ->
            val ingredients = databaseService.getRecipeIngredients(recipe.id)
            val matchCount = ingredients.count { ri ->
                val pi = pantryItems[ri.ingredientId]
                pi != null && pi.currentQuantity >= ri.requiredQuantity
            }
            val score = if (ingredients.isEmpty()) 0.0 else matchCount.toDouble() / ingredients.size
            recipe to score
        }.sortedByDescending { it.second }

        adapter.updateData(scored)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
