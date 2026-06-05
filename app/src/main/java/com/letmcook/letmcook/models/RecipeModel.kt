package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asInt
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class RecipeModel(
    val id: String,
    var ownerId: String,
    var title: String,
    var instructions: String? = null,
    var totalCalories: Int? = null,
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
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = RecipeModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            title = map["title"]?.toString().orEmpty(),
            instructions = map["instructions"]?.toString(),
            totalCalories = map["total_calories"].asInt(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
