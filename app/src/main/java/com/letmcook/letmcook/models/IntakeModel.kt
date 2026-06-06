package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asDouble
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class IntakeModel(
    val id: String,
    var ownerId: String,
    var recipeId: String? = null,
    var ingredientId: String? = null,
    var quantity: Double = 1.0,
    var date: String,
    var calories: Double = 0.0,
    var protein: Double = 0.0,
    var carbs: Double = 0.0,
    var fat: Double = 0.0,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "recipe_id" to recipeId,
        "ingredient_id" to ingredientId,
        "quantity" to quantity,
        "date" to date,
        "calories" to calories,
        "protein" to protein,
        "carbs" to carbs,
        "fat" to fat,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = IntakeModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            recipeId = map["recipe_id"]?.toString(),
            ingredientId = map["ingredient_id"]?.toString(),
            quantity = map["quantity"].asDouble() ?: 1.0,
            date = map["date"]?.toString().orEmpty(),
            calories = map["calories"].asDouble() ?: 0.0,
            protein = map["protein"].asDouble() ?: 0.0,
            carbs = map["carbs"].asDouble() ?: 0.0,
            fat = map["fat"].asDouble() ?: 0.0,
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap())
    }
}
