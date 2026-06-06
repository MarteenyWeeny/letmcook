package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asDouble
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class RecipeModel(
    val id: String,
    var ownerId: String,
    var title: String,
    var instructions: String? = null,
    var totalCalories: Double = 0.0,
    var totalProtein: Double = 0.0,
    var totalCarbs: Double = 0.0,
    var totalFat: Double = 0.0,
    var imageUrl: String? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "title" to title,
        "instructions" to instructions,
        "total_calories" to totalCalories,
        "total_protein" to totalProtein,
        "total_carbs" to totalCarbs,
        "total_fat" to totalFat,
        "image_url" to imageUrl,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    fun isValid(ingredients: List<RecipeIngredientModel>): Boolean {
        return title.isNotBlank() && ingredients.isNotEmpty()
    }

    companion object {
        fun fromMap(map: Map<String, Any?>) = RecipeModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            title = map["title"]?.toString().orEmpty(),
            instructions = map["instructions"]?.toString(),
            totalCalories = map["total_calories"].asDouble() ?: 0.0,
            totalProtein = map["total_protein"].asDouble() ?: 0.0,
            totalCarbs = map["total_carbs"].asDouble() ?: 0.0,
            totalFat = map["total_fat"].asDouble() ?: 0.0,
            imageUrl = map["image_url"]?.toString(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
