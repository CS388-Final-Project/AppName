package com.example.cs388finalproject.model

data class Post(
    val uid: String = "",
    val username: String = "",
    val imagePath: String = "",
    val songId: String = "",
    val location: Map<String, Double> = mapOf("lat" to 0.0, "lng" to 0.0),
    val createdAt: Long = System.currentTimeMillis()
)