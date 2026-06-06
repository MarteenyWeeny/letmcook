package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asInt
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class WorkoutModel(
    val id: String,
    var ownerId: String,
    var title: String,
    var type: String, // e.g., Cardio, Strength, Flexibility
    var durationMinutes: Int,
    var caloriesBurned: Int = 0,
    var description: String? = null,
    var isCompleted: Boolean = false,
    var scheduledDate: String, // yyyy-MM-dd
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "title" to title,
        "type" to type,
        "duration_minutes" to durationMinutes,
        "calories_burned" to caloriesBurned,
        "description" to description,
        "is_completed" to isCompleted,
        "scheduled_date" to scheduledDate,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = WorkoutModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            title = map["title"]?.toString().orEmpty(),
            type = map["type"]?.toString().orEmpty(),
            durationMinutes = map["duration_minutes"].asInt() ?: 0,
            caloriesBurned = map["calories_burned"].asInt() ?: 0,
            description = map["description"]?.toString(),
            isCompleted = map["is_completed"].asBoolean(),
            scheduledDate = map["scheduled_date"]?.toString().orEmpty(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )
    }
}
