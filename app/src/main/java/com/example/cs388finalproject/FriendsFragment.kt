package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs388finalproject.databinding.FragmentFriendsBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.model.UserProfile
import com.example.cs388finalproject.ui.auth.GuestSession
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.ui.home.RecommendedFriendsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

private enum class SearchMode {
    USERS,
    MUSIC
}

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: FriendsAdapter

    private val allUsers = mutableListOf<UserProfile>()
    private val allPosts = mutableListOf<Post>()
    private val friendIds = mutableSetOf<String>()
    private val recentUserSearches = mutableListOf<String>()

    private var searchMode: SearchMode = SearchMode.USERS
    private var currentQuery: String = ""
    private var currentUid: String? = null
    private var isGuestSession: Boolean = false

    // Checks if current session is guest
    private fun isGuest(): Boolean {
        return auth.currentUser?.isAnonymous == true || GuestSession.isGuest(requireContext())
    }

    // Inflates layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Sets up UI and loads data
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val guestFlag = GuestSession.isGuest(requireContext())
        val user = auth.currentUser

        if (guestFlag || user == null || user.isAnonymous) {
            isGuestSession = true
            binding.guestContainer.visibility = View.VISIBLE
            binding.etSearch.visibility = View.GONE
            binding.rgSearchMode.visibility = View.GONE
            binding.recyclerFriends.visibility = View.GONE
            binding.tvFriendsHint.visibility = View.VISIBLE
            binding.btnRecommendedFriends.visibility = View.GONE
            binding.tvFriendsHint.text = "Log in to search for friends."

            binding.btnExitGuestFriends.setOnClickListener {
                GuestSession.clearAll(requireContext())
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }

            currentUid = null
            return
        }

        isGuestSession = false
        currentUid = user.uid

        adapter = FriendsAdapter(
            onAddFriend = { profile -> addFriend(user.uid, profile.uid) },
            isActionAvailable = true
        )

        binding.recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFriends.adapter = adapter

        binding.btnRecommendedFriends.visibility = View.VISIBLE
        binding.btnRecommendedFriends.setOnClickListener {
            startActivity(Intent(requireContext(), RecommendedFriendsActivity::class.java))
        }

        setupSearchUi()
        binding.tvFriendsHint.text =
            "Start typing a username, song, or artist to find people."

        loadInitialData(currentUid)
    }

    // Sets up search mode and text listener
    private fun setupSearchUi() {
        binding.rbModeUsers.isChecked = true
        binding.rbModeMusic.isChecked = false
        searchMode = SearchMode.USERS

        binding.rgSearchMode.setOnCheckedChangeListener { _, checkedId ->
            searchMode =
                if (checkedId == binding.rbModeMusic.id) SearchMode.MUSIC else SearchMode.USERS
            applySearch()
        }

        binding.etSearch.addTextChangedListener { text ->
            currentQuery = text?.toString().orEmpty()
            applySearch()
        }
    }

    // Loads users, friends, and posts
    private fun loadInitialData(currentUid: String?) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    val isGuestUser = doc.getBoolean("isGuest") ?: false
                    if (isGuestUser) return@mapNotNull null
                    doc.toObject(UserProfile::class.java)
                }

                allUsers.clear()
                allUsers.addAll(users)
                applySearch()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load users.", Toast.LENGTH_SHORT).show()
            }

        if (currentUid != null && !isGuestSession) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    val friends = (doc.get("friends") as? List<String>) ?: emptyList()
                    friendIds.clear()
                    friendIds.addAll(friends)
                    applySearch()
                }
        }

        db.collection("posts")
            .orderBy("createdAt")
            .limit(200)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.toObjects(Post::class.java)
                allPosts.clear()
                allPosts.addAll(posts)
                applySearch()
            }
    }

    // Applies current search mode and query
    private fun applySearch() {
        val uid = currentUid
        val rawQuery = currentQuery.trim()
        val queryLower = rawQuery.lowercase()

        if (rawQuery.isBlank()) {
            adapter.submitList(emptyList())

            binding.tvFriendsHint.text = when (searchMode) {
                SearchMode.USERS -> {
                    if (recentUserSearches.isEmpty()) {
                        "Start typing a username to find people."
                    } else {
                        "Recent username searches: " +
                                recentUserSearches.joinToString(", ")
                    }
                }
                SearchMode.MUSIC ->
                    "Type a song, artist, or album to find users who posted it."
            }

            return
        }

        val results: List<UserProfile> = when (searchMode) {
            SearchMode.USERS -> {
                val candidates = allUsers.filter { u ->
                    val notFriend = if (uid != null && !isGuestSession) {
                        u.uid !in friendIds && u.uid != uid
                    } else true

                    notFriend && (
                            u.username.contains(queryLower, true) ||
                                    u.email.contains(queryLower, true)
                            )
                }

                if (candidates.isNotEmpty()) {
                    val first = candidates.first()
                    val nameForHistory =
                        first.username.ifBlank { first.email }.ifBlank { null }

                    if (nameForHistory != null) {
                        recentUserSearches.remove(nameForHistory)
                        recentUserSearches.add(0, nameForHistory)
                        if (recentUserSearches.size > 5) {
                            recentUserSearches.removeAt(recentUserSearches.lastIndex)
                        }
                    }
                }

                candidates
            }

            SearchMode.MUSIC -> {
                val matchingUids = allPosts.filter { p ->
                    p.songName.contains(queryLower, true) ||
                            p.artistName.contains(queryLower, true) ||
                            p.albumName.contains(queryLower, true)
                }
                    .map { it.uid }
                    .distinct()

                allUsers.filter { u ->
                    val isFriend = uid != null && !isGuestSession && u.uid in friendIds
                    !isFriend && u.uid in matchingUids
                }
            }
        }

        adapter.submitList(results)

        binding.tvFriendsHint.text = when {
            results.isEmpty() && searchMode == SearchMode.USERS ->
                "No users found for \"$rawQuery\"."
            results.isEmpty() && searchMode == SearchMode.MUSIC ->
                "No users found who posted music matching \"$rawQuery\"."
            searchMode == SearchMode.USERS ->
                "User results for \"$rawQuery\"."
            else ->
                "Users who posted music matching \"$rawQuery\"."
        }
    }

    // Adds a friend in Firestore
    private fun addFriend(currentUid: String, friendUid: String) {
        db.collection("users").document(currentUid)
            .update("friends", FieldValue.arrayUnion(friendUid))
            .addOnSuccessListener {
                friendIds.add(friendUid)
                applySearch()
                Toast.makeText(requireContext(), "Friend added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add friend.", Toast.LENGTH_SHORT).show()
            }
    }

    // Cleans up binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter for showing friends in a simple list
private class FriendsAdapter(
    private val onAddFriend: (UserProfile) -> Unit,
    private val isActionAvailable: Boolean
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val items = mutableListOf<UserProfile>()

    // Updates adapter list
    fun submitList(users: List<UserProfile>) {
        items.clear()
        items.addAll(users)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        // Binds one user row
        fun bind(user: UserProfile) {
            text1.text = if (user.username.isNotBlank()) user.username else user.email
            text2.text = if (isActionAvailable) "Tap to add friend" else ""
            itemView.setOnClickListener { onAddFriend(user) }
        }
    }

    // Creates view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return FriendViewHolder(view)
    }

    // Binds view holder
    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Returns list size
    override fun getItemCount(): Int = items.size
}
