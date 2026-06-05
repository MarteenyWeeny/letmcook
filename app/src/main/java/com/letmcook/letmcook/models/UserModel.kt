package com.letmcook.letmcook.models

import com.letmcook.letmcook.utils.asBoolean
import com.letmcook.letmcook.utils.toMap
import org.json.JSONObject

data class UserModel(
    val id: String,
    var email: String,
    var displayName: String? = null,
    var fullName: String? = null,
    var role: Int? = null,
    var isDeleted: Boolean = false,
    var isSynchronized: Boolean = false,
    var createdAt: String
) {
    fun toMap() = mapOf(
        "id" to id,
        "email" to email,
        "display_name" to displayName,
        "full_name" to fullName,
        "role" to role,
        "is_deleted" to isDeleted,
        "is_synchronized" to isSynchronized,
        "created_at" to createdAt
    )

    fun toJson() = JSONObject(toMap()).toString()

    companion object {
        fun fromMap(map: Map<String, Any?>) = UserModel(
            id = map["id"]?.toString().orEmpty(),
            email = map["email"]?.toString().orEmpty(),
            displayName = map["display_name"]?.toString(),
            fullName = map["full_name"]?.toString(),
            role = map["role"]?.toString()?.toIntOrNull(),
            isDeleted = map["is_deleted"].asBoolean(),
            isSynchronized = map["is_synchronized"].asBoolean(),
            createdAt = map["created_at"]?.toString().orEmpty()
        )

        fun fromJson(source: String) =
            fromMap(JSONObject(source).toMap() as Map<String, Any?>)
    }
}
