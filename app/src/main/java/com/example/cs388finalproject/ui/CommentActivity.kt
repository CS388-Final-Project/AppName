package com.example.cs388finalproject.ui

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CommentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "postId"
        const val EXTRA_POST_OWNER_ID = "postOwnerId" // Add this to know who owns the post
        private const val TAG = "CommentActivity"
    }

    private lateinit var recycler: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSend: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CommentAdapter
    private var postId: String? = null
    private var postOwnerId: String? = null // Track who owns the post

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        postId = intent.getStringExtra(EXTRA_POST_ID)
        postOwnerId = intent.getStringExtra(EXTRA_POST_OWNER_ID) // Get post owner ID

        if (postOwnerId.isNullOrEmpty() && !postId.isNullOrEmpty()) {
            fetchPostOwnerId()
        }

        recycler = findViewById(R.id.recyclerComments)
        etComment = findViewById(R.id.etComment)
        btnSend = findViewById(R.id.btnSendComment)

        adapter = CommentAdapter(
            postOwnerId = postOwnerId, // Pass post owner ID to adapter
            onDeleteComment = { commentId ->
                showDeleteConfirmation(commentId)
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnSend.setOnClickListener { sendComment() }
        etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else false
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBackComments)
        btnBack.setOnClickListener {
            finish() // Close the activity and return to previous screen
        }

        loadComments()
    }

    private fun fetchPostOwnerId() {
        val pid = postId ?: return
        db.collection("posts").document(pid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    postOwnerId = document.getString("uid") // Assuming 'uid' field
                    // Update adapter with new postOwnerId
                    adapter.updatePostOwnerId(postOwnerId)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch post owner ID", e)
            }
    }

    private fun loadComments() {
        val pid = postId ?: return
        db.collection("posts").document(pid)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    Log.e(TAG, "Error loading comments", err)
                    return@addSnapshotListener
                }
                val list = snap.toObjects(Comment::class.java)
                adapter.submitList(list)
            }
    }

    private fun sendComment() {
        val text = etComment.text.toString().trim()
        if (text.isEmpty()) return

        val user = auth.currentUser ?: run {
            Toast.makeText(this, "You must be logged in to comment", Toast.LENGTH_SHORT).show()
            return
        }

        val pid = postId ?: return

        // SIMPLIFIED: Match the ProfileFragment approach
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    Log.w(TAG, "User document doesn't exist for UID: ${user.uid}")
                    // Create a basic user document if it doesn't exist
                    createUserDocumentAndComment(user, pid, text)
                    return@addOnSuccessListener
                }

                // This should match exactly what ProfileFragment does
                val username = userDoc.getString("username") ?: "User"

                // Debug logging
                Log.d(TAG, "Retrieved username: $username")

                createComment(pid, user.uid, username, text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user document", e)
                // Fallback: use email as username
                val username = user.email ?: "User"
                createComment(pid, user.uid, username, text)
            }
    }

    private fun createUserDocumentAndComment(user: com.google.firebase.auth.FirebaseUser, postId: String, text: String) {
        // Create a basic user document with available info
        val userData = hashMapOf(
            "username" to (user.displayName ?: user.email?.split("@")?.firstOrNull() ?: "User"),
            "email" to user.email,
            "uid" to user.uid
        )

        db.collection("users").document(user.uid).set(userData)
            .addOnSuccessListener {
                val username = userData["username"] ?: "User"
                createComment(postId, user.uid, username, text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create user document", e)
                val username = user.email ?: "User"
                createComment(postId, user.uid, username, text)
            }
    }

    private fun createComment(postId: String, uid: String, username: String, text: String) {
        val docRef = db.collection("posts").document(postId).collection("comments").document()
        val comment = Comment(
            commentId = docRef.id,
            uid = uid,
            username = username,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        docRef.set(comment)
            .addOnSuccessListener {
                Log.d(TAG, "Comment posted successfully with username: $username")
                // increment comment count on parent post
                db.collection("posts").document(postId)
                    .update("commentCount", FieldValue.increment(1))
                etComment.setText("")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to post comment", e)
                Toast.makeText(
                    this,
                    "Failed to post comment: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showDeleteConfirmation(commentId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                deleteComment(commentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteComment(commentId: String) {
        val pid = postId ?: return
        val currentUserId = auth.currentUser?.uid ?: return

        // First, get the comment to verify ownership
        db.collection("posts").document(pid)
            .collection("comments")
            .document(commentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val comment = document.toObject(Comment::class.java)

                    // Verify the current user is the comment owner
                    if (comment?.uid == currentUserId) {
                        // Delete the comment
                        db.collection("posts").document(pid)
                            .collection("comments")
                            .document(commentId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "Comment deleted successfully")

                                // Decrement comment count
                                db.collection("posts").document(pid)
                                    .update("commentCount", FieldValue.increment(-1))
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to update comment count", e)
                                    }

                                Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete comment", e)
                                Toast.makeText(
                                    this,
                                    "Failed to delete comment: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "You can only delete your own comments",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch comment", e)
                Toast.makeText(
                    this,
                    "Failed to delete comment",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}