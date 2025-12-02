package com.example.cs388finalproject.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val bio: String = "Your Bio Here",
    val createdAt: Long = System.currentTimeMillis(),
    val isGuest: Boolean = false
)