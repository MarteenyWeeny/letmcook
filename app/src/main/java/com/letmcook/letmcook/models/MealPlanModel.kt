package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class MealPlanModel(
    val id: String,
    var ownerId: String,
    var recipeId: String,
    var plannedDate: String,
    var mealType: String? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "recipe_id" to recipeId,
        "planned_date" to plannedDate,
        "meal_type" to mealType,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = MealPlanModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            recipeId = map["recipe_id"]?.toString().orEmpty(),
            plannedDate = map["planned_date"]?.toString().orEmpty(),
            mealType = map["meal_type"]?.toString(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
