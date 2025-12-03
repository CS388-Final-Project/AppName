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

class MyPostsActivity : AppCompatActivity() {

    private lateinit var recyclerMyPosts: RecyclerView
    private lateinit var btnBack: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: MyPostsAdapter

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

        db.collection("posts")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "You haven't made any posts yet.", Toast.LENGTH_SHORT).show()
                    adapter.submitList(emptyList())
                    return@addOnSuccessListener
                }

                val posts = snapshot.toObjects(Post::class.java)
                    .sortedByDescending { it.createdAt }

                adapter.submitList(posts)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load your posts.", Toast.LENGTH_SHORT).show()
            }
    }
}