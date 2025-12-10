package com.example.cs388finalproject.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyPostsActivity : AppCompatActivity() {

    private lateinit var recyclerMyPosts: RecyclerView
    private lateinit var btnBack: ImageView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: MyPostsAdapter

    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        recyclerMyPosts = findViewById(R.id.recyclerMyPosts)
        btnBack = findViewById(R.id.btnBackMyPosts)

        adapter = MyPostsAdapter()
        recyclerMyPosts.layoutManager = LinearLayoutManager(this)
        recyclerMyPosts.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadMyPosts()
    }

    private fun loadMyPosts() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Use addSnapshotListener for real-time updates
        postsListener = db.collection("posts")
            .whereEqualTo("uid", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load your posts.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Toast.makeText(this, "You haven't made any posts yet.", Toast.LENGTH_SHORT).show()
                    adapter.submitList(emptyList())
                    return@addSnapshotListener
                }

                // Map documents and include the document ID as postId
                val posts = snapshot.documents.map { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id) ?: Post()
                }.sortedByDescending { it.createdAt }

                adapter.submitList(posts)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener when activity is destroyed
        postsListener?.remove()
    }
}