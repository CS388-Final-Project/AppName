package com.example.cs388finalproject.ui.home

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs388finalproject.R
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class RecommendedFriendRow(
    val user: UserProfile,
    val overlapCount: Int
)

class RecommendedFriendsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnBack: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: RecommendedFriendsAdapter

    // Sets up UI and starts load
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommended_friends)

        recycler = findViewById(R.id.recyclerRecommendedFriends)
        btnBack = findViewById(R.id.btnBackRecommendedFriends)

        adapter = RecommendedFriendsAdapter { profile ->
            addFriend(profile)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadRecommendedFriends()
    }

    // Loads recommended friends based on song/artist overlap
    private fun loadRecommendedFriends() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("posts")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { mySnap ->
                val myPosts = mySnap.toObjects(Post::class.java)
                if (myPosts.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Post some songs first to get recommendations.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val mySongIds = myPosts
                    .map { it.songId }
                    .filter { it.isNotBlank() }
                    .toSet()

                val myArtists = myPosts
                    .map { it.artistName.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

                db.collection("posts")
                    .get()
                    .addOnSuccessListener { allSnap ->
                        val allPosts = allSnap.toObjects(Post::class.java)

                        val otherPostsCount = allPosts.count { it.uid != user.uid }




                        val overlapCounts = mutableMapOf<String, Int>()

                        for (p in allPosts) {
                            if (p.uid == user.uid) continue

                            //val songKey = p.songName.trim().lowercase()
                            val artistKey = p.artistName.trim().lowercase()

                            val sameSong = p.songId.isNotBlank() && p.songId in mySongIds



                            val sameArtist = myArtists.any { artist ->
                                artistKey.contains(artist) || artist.contains(artistKey)
                            }

                            if (sameSong || sameArtist) {
                                overlapCounts[p.uid] = (overlapCounts[p.uid] ?: 0) + 1
                            }
                        }



                        if (overlapCounts.isEmpty()) {
                            val otherUids = allPosts.map{ it.uid }.filter{it != user.uid}.distinct()

                            if (otherUids.isEmpty()){
                                Toast.makeText(
                                    this,
                                    "No recommended friends yet. Keep posting songs!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                adapter.submitList(emptyList())
                                return@addOnSuccessListener
                            }
                            otherUids.forEach { uidOther ->
                                overlapCounts[uidOther] = overlapCounts[uidOther] ?: 1
                            }

                        }

                        val topUids = overlapCounts.entries
                            .sortedByDescending { it.value }
                            .take(10)
                            .map { it.key }

                        db.collection("users")
                            .whereIn("uid", topUids)
                            .get()
                            .addOnSuccessListener { userSnap ->
                                val profiles = userSnap.toObjects(UserProfile::class.java)

                                val rows = profiles.map { profile ->
                                    RecommendedFriendRow(
                                        user = profile,
                                        overlapCount = overlapCounts[profile.uid] ?: 0
                                    )
                                }.sortedByDescending { it.overlapCount }

                                adapter.submitList(rows)
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Failed to load recommended friends.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to compute recommendations.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to load your posts.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Adds selected recommended friend
    private fun addFriend(profile: UserProfile) {
        val current = auth.currentUser ?: return

        db.collection("users").document(current.uid)
            .update("friends", FieldValue.arrayUnion(profile.uid))
            .addOnSuccessListener {
                val name = profile.username.ifBlank { profile.email }
                Toast.makeText(
                    this,
                    "Added ${name} as a friend!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add friend.", Toast.LENGTH_SHORT).show()
            }
    }
}

// Adapter for recommended friends list
private class RecommendedFriendsAdapter(
    private val onAddFriend: (UserProfile) -> Unit
) : RecyclerView.Adapter<RecommendedFriendsAdapter.ViewHolder>() {

    private val items = mutableListOf<RecommendedFriendRow>()

    // Updates adapter list
    fun submitList(rows: List<RecommendedFriendRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        // Binds one recommended friend row
        fun bind(row: RecommendedFriendRow) {
            val user = row.user
            val name = if (user.username.isNotBlank()) user.username else user.email
            text1.text = name
            text2.text = "Shared songs/artists: ${row.overlapCount}"
            itemView.setOnClickListener { onAddFriend(user) }
        }
    }

    // Creates view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    // Binds view holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Returns list size
    override fun getItemCount(): Int = items.size
}
