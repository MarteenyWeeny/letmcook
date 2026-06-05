package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class IngredientModel(
    val id: String,
    var ownerId: String,
    var name: String,
    var category: String? = null,
    var unitOfMeasure: String? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "name" to name,
        "category" to category,
        "unit_of_measure" to unitOfMeasure,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = IngredientModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            name = map["name"]?.toString().orEmpty(),
            category = map["category"]?.toString(),
            unitOfMeasure = map["unit_of_measure"]?.toString(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
