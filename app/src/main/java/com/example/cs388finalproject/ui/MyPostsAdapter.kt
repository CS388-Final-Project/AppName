package com.example.cs388finalproject.ui

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsAdapter :
    ListAdapter<Post, MyPostsAdapter.MyPostViewHolder>(DiffCallback) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    object DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            // Use postId for comparison
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_post, parent, false)
        return MyPostViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAlbum: ImageView = itemView.findViewById(R.id.imagePost)
        private val tvSong: TextView = itemView.findViewById(R.id.textSong)
        private val tvArtist: TextView = itemView.findViewById(R.id.tv_card_description)
        private val tvLocation: TextView = itemView.findViewById(R.id.textLocation)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_card_title)

        // Like button components
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)

        // Comment button components
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        private val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

        fun bind(post: Post) {
            val postId = post.postId
            val user = auth.currentUser

            // Validate postId exists
            if (postId.isBlank()) {
                tvTimestamp.text = "Error: Invalid post"
                return
            }

            tvSong.text = post.songName
            tvArtist.text = post.artistName

            // Simple location display if you stored lat/lng map
            val locMap = post.location
            if (locMap.isNotEmpty() && locMap["lat"] != null && locMap["lng"] != null) {
                tvLocation.text = "Location: ${locMap["lat"]}, ${locMap["lng"]}"
                tvLocation.visibility = View.VISIBLE
            } else {
                tvLocation.visibility = View.GONE
            }

            tvTimestamp.text = formatter.format(Date(post.createdAt))

            if (post.albumArtUrl.isNotBlank()) {
                Glide.with(itemView)
                    .load(post.albumArtUrl)
                    .centerCrop()
                    .into(imgAlbum)
            } else if (post.imagePath.isNotBlank()) {
                Glide.with(itemView)
                    .load(post.imagePath)
                    .centerCrop()
                    .into(imgAlbum)
            } else {
                imgAlbum.setImageResource(R.drawable.sharp_add_photo_alternate_24)
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmation(post)
            }

            val likedBy = post.likedBy
            val likeCount = post.likeCount
            val currentUid = user?.uid

            tvLikeCount.text = likeCount.toString()

            val userLiked = likedBy.contains(currentUid)
            btnLike.setImageResource(
                if (userLiked) R.drawable.filled_favorite_24
                else R.drawable.outline_favorite_24
            )

            btnLike.setOnClickListener {
                if (currentUid == null) return@setOnClickListener

                val postRef = db.collection("posts").document(postId)

                if (!userLiked) {
                    // Like
                    postRef.update(
                        mapOf(
                            "likedBy" to FieldValue.arrayUnion(currentUid),
                            "likeCount" to likeCount + 1
                        )
                    )
                } else {
                    // Unlike
                    postRef.update(
                        mapOf(
                            "likedBy" to FieldValue.arrayRemove(currentUid),
                            "likeCount" to (likeCount - 1).coerceAtLeast(0)
                        )
                    )
                }
            }

            btnComment.setOnClickListener {
                val intent = Intent(itemView.context, CommentActivity::class.java)
                intent.putExtra(CommentActivity.EXTRA_POST_ID, postId)
                itemView.context.startActivity(intent)
            }

            // Count comments dynamically
            db.collection("posts")
                .document(postId)
                .collection("comments")
                .addSnapshotListener { snapshot, _ ->
                    val count = snapshot?.size() ?: 0
                    tvCommentCount.text = count.toString()
                }
        }

        private fun showDeleteConfirmation(post: Post) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    deletePost(post)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun deletePost(post: Post) {
            val postId = post.postId
            if (postId.isBlank()) {
                Toast.makeText(itemView.context, "Cannot delete: Invalid post ID", Toast.LENGTH_SHORT).show()
                return
            }

            // Delete the post document from Firestore
            db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    // The list will automatically update via the snapshot listener in MyPostsActivity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}