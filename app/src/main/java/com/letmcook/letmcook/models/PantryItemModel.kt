package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.asDouble
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class PantryItemModel(
    val id: String,
    var ownerId: String,
    var ingredientId: String,
    var currentQuantity: Double = 0.0,
    var expirationDate: String? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "owner_id" to ownerId,
        "ingredient_id" to ingredientId,
        "current_quantity" to currentQuantity,
        "expiration_date" to expirationDate,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = PantryItemModel(
            id = map["id"]?.toString().orEmpty(),
            ownerId = map["owner_id"]?.toString().orEmpty(),
            ingredientId = map["ingredient_id"]?.toString().orEmpty(),
            currentQuantity = map["current_quantity"].asDouble() ?: 0.0,
            expirationDate = map["expiration_date"]?.toString(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
