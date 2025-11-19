package com.example.cs388finalproject.data

import com.example.cs388finalproject.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Query for the main feed, newest first */
    fun feed(limit: Long = 50): Query =
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
}