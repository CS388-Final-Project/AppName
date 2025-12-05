package com.example.cs388finalproject.model

data class Comment(
    val commentId: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
