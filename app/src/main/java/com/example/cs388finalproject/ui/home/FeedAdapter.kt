package com.example.cs388finalproject.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.databinding.PostItemBinding

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    private val items = mutableListOf<Post>()

    fun submitList(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class PostViewHolder(
        private val binding: PostItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // 👇 1) Username in the card title
            val username = if (post.username.isNotBlank()) {
                post.username
            } else {
                // placeholder for now – later you’ll store real username in the post
                "Unknown user"
            }
            binding.tvCardTitle.text = username

            // 👇 2) Hide text description so the photo is the “body” of the card
            binding.tvCardDescription.visibility = View.GONE

            // 👇 3) Photo uploaded by the user
            Glide.with(binding.root)
                .load(post.imagePath)   // should be a download URL or storage path you resolve
                .into(binding.imagePost)

            // 👇 4) Song name (placeholder until you have real titles)
            binding.textSong.text = post.songId.ifBlank { "Unknown song" }

            // 👇 5) Location (simple lat/lng for now)
            val lat = post.location["lat"] ?: 0.0
            val lng = post.location["lng"] ?: 0.0
            binding.textLocation.text =
                "(${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
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
