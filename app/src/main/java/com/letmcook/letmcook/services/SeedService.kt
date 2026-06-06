package com.letmcook.letmcook.services

import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.RecipeIngredientModel
import com.letmcook.letmcook.models.RecipeModel
import com.letmcook.letmcook.models.WorkoutModel
import java.text.SimpleDateFormat
import java.util.*

class SeedService(private val databaseService: DatabaseService) {

    fun seedData() {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Seed Ingredients if missing
        val existingIngredients = databaseService.getAllIngredients()
        val ingredientsMap = mutableMapOf<String, IngredientModel>()
        existingIngredients.forEach { ingredientsMap[it.name] = it }

        fun addIng(name: String, category: String, unit: String, cal: Double, pro: Double, carb: Double, fat: Double): IngredientModel {
            if (ingredientsMap.containsKey(name)) return ingredientsMap[name]!!
            val ing = IngredientModel(UUID.randomUUID().toString(), "system", name, category, unit, cal, pro, carb, fat, createdAt = timestamp)
            ingredientsMap[name] = ing
            databaseService.upsertIngredient(ing)
            return ing
        }

        addIng("Chicken Breast", "Meat", "g", 1.65, 0.31, 0.0, 0.036)
        addIng("Rice", "Grains", "g", 1.3, 0.027, 0.28, 0.003)
        addIng("Eggs", "Dairy", "unit", 70.0, 6.0, 0.6, 5.0)
        addIng("Onion", "Vegetables", "g", 0.4, 0.011, 0.09, 0.001)
        addIng("Spinach", "Vegetables", "g", 0.23, 0.029, 0.036, 0.004)
        addIng("Milk", "Dairy", "ml", 0.42, 0.034, 0.05, 0.01)
        addIng("Pasta", "Grains", "g", 1.31, 0.05, 0.25, 0.01)
        addIng("Banana", "Fruit", "unit", 89.0, 1.1, 22.8, 0.3)
        addIng("Peanut Butter", "Pantry", "g", 5.88, 0.25, 0.2, 0.5)
        addIng("Oats", "Grains", "g", 3.89, 0.17, 0.66, 0.07)
        addIng("Honey", "Pantry", "g", 3.04, 0.003, 0.82, 0.0)
        addIng("Greek Yogurt", "Dairy", "g", 0.59, 0.1, 0.036, 0.004)
        addIng("Chickpeas", "Legumes", "g", 1.64, 0.09, 0.27, 0.026)
        addIng("Avocado", "Fruit", "unit", 160.0, 2.0, 8.5, 14.7)
        addIng("Lemon", "Fruit", "unit", 17.0, 0.6, 5.4, 0.2)
        addIng("Whole-grain Bread", "Grains", "slice", 69.0, 3.6, 12.0, 0.9)
        addIng("Quinoa", "Grains", "g", 1.2, 0.04, 0.21, 0.02)
        addIng("Black Beans", "Legumes", "g", 1.32, 0.09, 0.24, 0.005)
        addIng("Salsa", "Pantry", "g", 0.36, 0.015, 0.07, 0.002)
        addIng("Cucumber", "Vegetables", "g", 0.15, 0.007, 0.036, 0.001)
        addIng("Kidney Beans", "Legumes", "g", 1.27, 0.09, 0.23, 0.005)
        addIng("Chopped Tomatoes", "Vegetables", "g", 0.18, 0.009, 0.039, 0.002)
        addIng("Salmon Fillet", "Fish", "g", 2.08, 0.2, 0.0, 0.13)
        addIng("Pesto", "Pantry", "g", 4.5, 0.05, 0.05, 0.45)
        addIng("Broccoli", "Vegetables", "g", 0.34, 0.028, 0.07, 0.004)

        // Seed Recipes if missing
        val existingRecipes = databaseService.getAllRecipes()
        fun addRecipe(title: String, instructions: String, cals: Double, pro: Double, carb: Double, fat: Double, ings: List<Pair<String, Double>>) {
            if (existingRecipes.any { it.title == title }) return
            val recipeId = UUID.randomUUID().toString()
            val recipe = RecipeModel(recipeId, "system", title, instructions, cals, pro, carb, fat, createdAt = timestamp)
            val recipeIngredients = ings.map { (name, qty) ->
                RecipeIngredientModel(UUID.randomUUID().toString(), "system", recipeId, ingredientsMap[name]?.id ?: "", qty, createdAt = timestamp)
            }
            databaseService.upsertRecipeWithIngredients(recipe, recipeIngredients)
        }

        addRecipe("Chicken Fried Rice", "1. Cook rice.\n2. Sauté onion and chicken.\n3. Mix in rice.", 450.0, 35.0, 50.0, 12.0, listOf("Chicken Breast" to 150.0, "Rice" to 200.0, "Onion" to 50.0))
        addRecipe("3-Ingredient Banana Pancakes", "1. Mash banana in a bowl.\n2. Whisk in eggs and peanut butter.\n3. Cook in a skillet.", 350.0, 15.0, 25.0, 18.0, listOf("Banana" to 1.0, "Eggs" to 2.0, "Peanut Butter" to 15.0))
        addRecipe("Overnight Oats", "1. Combine ingredients in a jar.\n2. Refrigerate for 6 hours.", 300.0, 12.0, 45.0, 8.0, listOf("Oats" to 50.0, "Milk" to 120.0, "Honey" to 10.0, "Greek Yogurt" to 60.0))
        addRecipe("Smashed Chickpea Avocado Sandwich", "1. Mash chickpeas and avocado.\n2. Spread onto bread.", 400.0, 14.0, 40.0, 20.0, listOf("Chickpeas" to 100.0, "Avocado" to 0.5, "Lemon" to 0.5, "Whole-grain Bread" to 2.0))
        addRecipe("15-Minute Bean Chili", "1. Sauté onion.\n2. Add beans and tomatoes.\n3. Simmer for 10 minutes.", 320.0, 18.0, 55.0, 2.0, listOf("Kidney Beans" to 200.0, "Chopped Tomatoes" to 200.0, "Onion" to 1.0))
        addRecipe("Pesto Salmon & Broccoli", "1. Spread pesto on salmon.\n2. Bake with broccoli at 400°F for 15 mins.", 450.0, 35.0, 10.0, 30.0, listOf("Salmon Fillet" to 150.0, "Pesto" to 30.0, "Broccoli" to 150.0))

        // Seed Workouts if missing
        val existingWorkouts = databaseService.getWorkoutsForDate("system", today)
        if (existingWorkouts.isEmpty()) {
            val workouts = listOf(
                WorkoutModel(UUID.randomUUID().toString(), "system", "Morning Run", "Cardio", 30, 300, "A brisk 5km run.", false, today, timestamp),
                WorkoutModel(UUID.randomUUID().toString(), "system", "Strength Training", "Strength", 45, 250, "Focus on full body.", false, today, timestamp),
                WorkoutModel(UUID.randomUUID().toString(), "system", "Yoga Flow", "Flexibility", 20, 80, "Daily stretching.", false, today, timestamp)
            )
            workouts.forEach { databaseService.upsertWorkout(it) }
        }
    }
}
