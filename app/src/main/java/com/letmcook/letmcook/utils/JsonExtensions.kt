package com.letmcook.letmcook.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    val keysItr = keys()
    while (keysItr.hasNext()) {
        val key = keysItr.next()
        var value = get(key)
        if (value is JSONArray) {
            value = value.toList()
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        map[key] = if (value == JSONObject.NULL) null else value
    }
    return map
}

fun JSONArray.toList(): List<Any?> {
    val list = mutableListOf<Any?>()
    for (i in 0 until length()) {
        var value = get(i)
        if (value is JSONArray) {
            value = value.toList()
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        list.add(if (value == JSONObject.NULL) null else value)
    }
    return list
}

fun Any?.asBoolean(): Boolean = when (this) {
    is Boolean -> this
    is Number -> toInt() != 0
    null -> false
    else -> toString().toBoolean()
}

fun Any?.asInt(): Int? = when (this) {
    is Number -> toInt()
    null -> null
    else -> toString().toIntOrNull()
}

fun Any?.asDouble(): Double? = when (this) {
    is Number -> toDouble()
    null -> null
    else -> toString().toDoubleOrNull()
}

fun extractMappedValue(source: Map<String, Any?>, aliases: List<String>): Any? {
    val missingValue = Any() // Internal marker
    if (aliases.isEmpty()) {
        return missingValue
    }

    for (alias in aliases) {
        if (source.containsKey(alias)) {
            return source[alias]
        }
    }

    return missingValue
}

/**
 * Validates if a recipe has at least one ingredient.
 */
fun validateRecipeIngredients(ingredients: List<Any?>): Boolean {
    return ingredients.isNotEmpty()
}
