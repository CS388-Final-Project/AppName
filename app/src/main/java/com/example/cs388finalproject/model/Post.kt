package com.example.cs388finalproject.model

data class Post(
    val postId: String = "",
    val uid: String = "",
    val username: String = "",

    // Image (placeholder for now until camera/gallery is added)
    val imagePath: String = "",

    // Spotify metadata
    val songId: String = "",
    val songName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val albumArtUrl: String = "",
    val durationMs: Int = 0,
    val explicit: Boolean = false,
    val previewUrl: String = "",

    // Location
    val location: Map<String, Double> = mapOf("lat" to 0.0, "lng" to 0.0),

    // Feed ordering
    val createdAt: Long = System.currentTimeMillis(),

    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0
)