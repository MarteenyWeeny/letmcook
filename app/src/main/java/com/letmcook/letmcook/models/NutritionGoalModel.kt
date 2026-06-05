package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asInt
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class NutritionGoalModel(
    val id: String,
    var ownerId: String,
    var dailyCalorieTarget: Int? = null,
    var proteinTargetGrams: Int? = null,
    var carbTargetGrams: Int? = null,
    var fatTargetGrams: Int? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "daily_calorie_target" to dailyCalorieTarget,
        "protein_target_grams" to proteinTargetGrams,
        "carb_target_grams" to carbTargetGrams,
        "fat_target_grams" to fatTargetGrams,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = NutritionGoalModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            dailyCalorieTarget = map["daily_calorie_target"].asInt(),
            proteinTargetGrams = map["protein_target_grams"].asInt(),
            carbTargetGrams = map["carb_target_grams"].asInt(),
            fatTargetGrams = map["fat_target_grams"].asInt(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
