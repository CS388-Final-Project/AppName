package com.example.cs388finalproject.ui

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private var postOwnerId: String? = null,
    private val onDeleteComment: (commentId: String) -> Unit,
) : ListAdapter<Comment, CommentAdapter.VH>(DIFF) {

    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    fun updatePostOwnerId(newPostOwnerId: String?) {
        postOwnerId = newPostOwnerId
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(old: Comment, new: Comment) = old.commentId == new.commentId
            override fun areContentsTheSame(old: Comment, new: Comment) = old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvCommentText)
        private val tvTime: TextView = view.findViewById(R.id.tvCommentTime)
        private val btnDelete: ImageView = view.findViewById(R.id.btnDeleteComment)
        private val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        fun bind(c: Comment) {
            // Format: "Username: comment text"
            val username = c.username.ifBlank { "Unknown" }
            val fullText = "$username: ${c.text}"

            // Make the username bold
            val spannable = SpannableString(fullText)
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                username.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            tvText.text = spannable
            tvTime.text = fmt.format(Date(c.createdAt))

            // Show delete button only for current user's comments
            btnDelete.visibility = if (c.uid == currentUserId) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set delete button click listener
            btnDelete.setOnClickListener {
                if (c.uid == currentUserId) {
                    onDeleteComment(c.commentId)
                }
            }
        }
    }
}