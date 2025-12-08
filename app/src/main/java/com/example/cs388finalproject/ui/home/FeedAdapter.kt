package com.example.cs388finalproject.ui.home

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cs388finalproject.databinding.PostItemBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.ui.CommentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class FeedAdapter(
    private val onSongClick: (Post) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    private val items = mutableListOf<Post>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun submitList(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Converts lat/lng → City, State
    private fun formatLocation(context: Context, lat: Double, lng: Double): String {
        return try {
            val geo = Geocoder(context, Locale.getDefault())
            val results: List<Address>? = geo.getFromLocation(lat, lng, 1)

            if (!results.isNullOrEmpty()) {
                val address = results[0]
                val city = address.locality ?: ""
                val state = address.adminArea ?: ""

                if (city.isNotBlank() || state.isNotBlank()) "$city, $state"
                else "Unknown Location"
            } else "Unknown Location"

        } catch (e: Exception) {
            "Unknown Location"
        }
    }

    inner class PostViewHolder(
        private val binding: PostItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {

            val user = auth.currentUser
            val postId = post.postId

            if (postId.isBlank()) {
                // Handle error - post doesn't have a valid ID
                binding.tvCardTitle.text = "Error: Invalid post"
                return
            }

            // Username
            binding.tvCardTitle.text = post.username.ifBlank { "Unknown user" }

            // Remove description from your design
            binding.tvCardDescription.visibility = View.GONE

            // Image
            Glide.with(binding.root)
                .load(post.imagePath)
                .into(binding.imagePost)

            // Song
            val songLine =
                if (post.songName.isNotBlank() || post.artistName.isNotBlank())
                    "${post.songName} — ${post.artistName}"
                else "Unknown song"

            binding.textSong.text = songLine
            binding.textSong.setOnClickListener { onSongClick(post) }

            // Click → open song details
            binding.textSong.setOnClickListener {
                onSongClick(post)
            }

            // Format location string
            val lat = post.location["lat"] ?: 0.0
            val lng = post.location["lng"] ?: 0.0
            binding.textLocation.text =
                formatLocation(binding.root.context, lat, lng)

            // -----------------------------
            //     LIKE BUTTON SETUP
            // -----------------------------
            val likedBy = post.likedBy
            val likeCount = post.likeCount
            val currentUid = user?.uid

            // Show like count
            binding.tvLikeCount.text = likeCount.toString()

            // Set heart state
            val userLiked = likedBy.contains(currentUid)
            binding.btnLike.setImageResource(
                if (userLiked) com.example.cs388finalproject.R.drawable.filled_favorite_24
                else com.example.cs388finalproject.R.drawable.outline_favorite_24
            )

            // Handle like click
            binding.btnLike.setOnClickListener {
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

            // -----------------------------
            //     COMMENT BUTTON SETUP
            // -----------------------------
            binding.btnComment.setOnClickListener {
                val intent = Intent(binding.root.context, CommentActivity::class.java)
                intent.putExtra(CommentActivity.EXTRA_POST_ID, postId)
                intent.putExtra(CommentActivity.EXTRA_POST_OWNER_ID, post.uid)
                binding.root.context.startActivity(intent)
            }

            // Count comments dynamically
            db.collection("posts")
                .document(postId)
                .collection("comments")
                .addSnapshotListener { snapshot, _ ->
                    val count = snapshot?.size() ?: 0
                    binding.tvCommentCount.text = count.toString()
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PostItemBinding.inflate(inflater, parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
