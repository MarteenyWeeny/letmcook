package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asDouble
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class RecipeIngredientModel(
    val id: String,
    var ownerId: String,
    var recipeId: String,
    var ingredientId: String,
    var requiredQuantity: Double,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "recipe_id" to recipeId,
        "ingredient_id" to ingredientId,
        "required_quantity" to requiredQuantity,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = RecipeIngredientModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            recipeId = map["recipe_id"]?.toString().orEmpty(),
            ingredientId = map["ingredient_id"]?.toString().orEmpty(),
            requiredQuantity = map["required_quantity"].asDouble() ?: 0.0,
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
