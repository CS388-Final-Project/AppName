package com.example.cs388finalproject.ui.home

import android.content.Context
import android.location.Geocoder
import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cs388finalproject.databinding.PostItemBinding
import com.example.cs388finalproject.model.Post
import java.util.Locale



class FeedAdapter(
    private val onSongClick: (Post) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    private val items = mutableListOf<Post>()

    fun submitList(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Converts lat/lng into City, State
    private fun formatLocation(context: Context, lat: Double, lng: Double): String {
        return try {
            val geo = Geocoder(context, Locale.getDefault())
            val results: List<Address>? = geo.getFromLocation(lat, lng, 1)

            if (!results.isNullOrEmpty()) {
                val address = results[0]
                val city = address.locality ?: ""
                val state = address.adminArea ?: ""   // <-- FIX: valid field name

                if (city.isNotBlank() || state.isNotBlank()) {
                    "$city, $state"
                } else {
                    "Unknown Location"
                }
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            "Unknown Location"
        }
    }

    inner class PostViewHolder(
        private val binding: PostItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {

            // Username
            val username = post.username.ifBlank { "Unknown user" }
            binding.tvCardTitle.text = username

            // Hide description
            binding.tvCardDescription.visibility = View.GONE

            // Post image
            Glide.with(binding.root)
                .load(post.imagePath)
                .into(binding.imagePost)

            // Song line
            val songLine =
                if (post.songName.isNotBlank() || post.artistName.isNotBlank())
                    "${post.songName} — ${post.artistName}"
                else
                    "Unknown song"

            binding.textSong.text = songLine

            // Click → open song details
            binding.textSong.setOnClickListener {
                onSongClick(post)
            }

            // Format location string
            val lat = post.location["lat"] ?: 0.0
            val lng = post.location["lng"] ?: 0.0
            binding.textLocation.text =
                formatLocation(binding.root.context, lat, lng)
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