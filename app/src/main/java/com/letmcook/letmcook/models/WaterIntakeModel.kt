package com.letmcook.letmcook.models

data class WaterIntakeModel(
    val id: String,
    var ownerId: String,
    var amountMl: Int,
    var date: String, // yyyy-MM-dd
    var createdAt: String
)
