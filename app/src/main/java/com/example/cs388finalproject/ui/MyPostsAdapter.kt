package com.example.cs388finalproject.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsAdapter :
    ListAdapter<Post, MyPostsAdapter.MyPostViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            // If your Post has an id field, use that; otherwise fallback to createdAt
            return oldItem.createdAt == newItem.createdAt && oldItem.songId == newItem.songId
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

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAlbum: ImageView = itemView.findViewById(R.id.imagePost)
        private val tvSong: TextView = itemView.findViewById(R.id.textSong)
        private val tvArtist: TextView = itemView.findViewById(R.id.tv_card_description)
        private val tvLocation: TextView = itemView.findViewById(R.id.textLocation)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_card_title)

        private val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

        fun bind(post: Post) {
            tvSong.text = post.songName
            tvArtist.text = post.artistName

            // simple location display if you stored lat/lng map
            val locMap = post.location
            if (locMap != null && locMap["lat"] != null && locMap["lng"] != null) {
                tvLocation.text = "Location: ${locMap["lat"]}, ${locMap["lng"]}"
                tvLocation.visibility = View.VISIBLE
            } else {
                tvLocation.visibility = View.GONE
            }

            tvTimestamp.text = formatter.format(Date(post.createdAt))

            if (!post.albumArtUrl.isNullOrEmpty()) {
                Glide.with(itemView)
                    .load(post.albumArtUrl)
                    .centerCrop()
                    .into(imgAlbum)
            } else if (!post.imagePath.isNullOrEmpty()) {
                Glide.with(itemView)
                    .load(post.imagePath)
                    .centerCrop()
                    .into(imgAlbum)
            } else {
                imgAlbum.setImageResource(R.drawable.sharp_add_photo_alternate_24)
            }
        }
    }
}