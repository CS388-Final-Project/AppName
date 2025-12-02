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
import com.example.cs388finalproject.ui.auth.LoginActivity
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

    // Data + state
    private val allUsers = mutableListOf<UserProfile>()
    private val allPosts = mutableListOf<Post>()
    private val friendIds = mutableSetOf<String>()
    private var searchMode: SearchMode = SearchMode.USERS
    private var currentQuery: String = ""
    private var currentUid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isGuest = com.example.cs388finalproject.ui.auth.GuestSession.isGuest(requireContext())

        val user = auth.currentUser
        if (isGuest || user == null || user.isAnonymous) {
            binding.guestContainer.visibility = View.VISIBLE
            binding.etSearch.visibility = View.GONE
            binding.rgSearchMode.visibility = View.GONE
            binding.recyclerFriends.visibility = View.GONE
            binding.tvFriendsHint.visibility = View.GONE

            binding.btnExitGuestFriends.setOnClickListener {
                com.example.cs388finalproject.ui.auth.GuestSession.clearAll(requireContext())
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }

            return
        }

        currentUid = user.uid

        adapter = FriendsAdapter { profile ->
            addFriend(user.uid, profile.uid)
        }

        binding.recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFriends.adapter = adapter

        setupSearchUi()
        loadInitialData(user.uid)
    }

    private fun setupSearchUi() {
        binding.rbModeUsers.isChecked = true
        binding.rbModeMusic.isChecked = false

        binding.rgSearchMode.setOnCheckedChangeListener { _, checkedId ->
            searchMode = if (checkedId == binding.rbModeMusic.id) {
                SearchMode.MUSIC
            } else {
                SearchMode.USERS
            }
            applySearch()
        }

        binding.etSearch.addTextChangedListener { text ->
            currentQuery = text?.toString().orEmpty()
            applySearch()
        }

        binding.tvFriendsHint.text = "Search for users to add as friends."
    }

    private fun loadInitialData(currentUid: String) {
        // Load ALL users
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    // Skip guest users
                    val isGuest = doc.getBoolean("isGuest") ?: false
                    if (isGuest) return@mapNotNull null

                    doc.toObject(UserProfile::class.java)
                }
                allUsers.clear()
                allUsers.addAll(users)
                applySearch()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load users.", Toast.LENGTH_SHORT).show()
            }

        // Load current user's existing friends
        db.collection("users").document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                val friends = (doc.get("friends") as? List<String>) ?: emptyList()
                friendIds.clear()
                friendIds.addAll(friends)
                applySearch()
            }

        // Load some posts for music search
        db.collection("posts")
            .orderBy("createdAt")
            .limit(200)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                allPosts.clear()
                allPosts.addAll(posts)
                applySearch()
            }
    }

    /** Filter results based on mode + query and EXCLUDE already-friends. */
    private fun applySearch() {
        val uid = currentUid ?: return
        val query = currentQuery.trim()
        val lower = query.lowercase()

        val results: List<UserProfile> = when (searchMode) {
            SearchMode.USERS -> {
                // Only non-friends + not yourself
                val candidates = allUsers.filter { u ->
                    u.uid != uid && u.uid !in friendIds
                }
                if (query.isBlank()) {
                    candidates
                } else {
                    candidates.filter { u ->
                        u.username.contains(lower, ignoreCase = true) ||
                                u.email.contains(lower, ignoreCase = true)
                    }
                }
            }

            SearchMode.MUSIC -> {
                if (query.isBlank()) {
                    emptyList()
                } else {
                    val matchingUids = allPosts.filter { p ->
                        p.songName.contains(lower, ignoreCase = true) ||
                                p.artistName.contains(lower, ignoreCase = true) ||
                                p.albumName.contains(lower, ignoreCase = true)
                    }.map { it.uid }
                        .distinct()

                    allUsers.filter { u ->
                        u.uid != uid && u.uid !in friendIds && u.uid in matchingUids
                    }
                }
            }
        }

        adapter.submitList(results)

        binding.tvFriendsHint.text = if (results.isEmpty()) {
            when {
                query.isBlank() && searchMode == SearchMode.MUSIC ->
                    "Type a song, artist, or album to find users."
                query.isBlank() ->
                    "Search for users to add as friends."
                else ->
                    "No results for \"$query\"."
            }
        } else {
            when (searchMode) {
                SearchMode.USERS -> "User results for \"$query\""
                SearchMode.MUSIC -> "Users who posted music matching \"$query\""
            }
        }
    }

    /** Only ADD friend here. Removal is done from the profile screen. */
    private fun addFriend(currentUid: String, friendUid: String) {
        db.collection("users").document(currentUid)
            .update("friends", FieldValue.arrayUnion(friendUid))
            .addOnSuccessListener {
                friendIds.add(friendUid)
                // Re-run search → newly-added friend disappears from results
                applySearch()
                Toast.makeText(requireContext(), "Friend added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add friend.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Simple adapter using android.R.layout.simple_list_item_2
 * - text1: username or email
 * - text2: "Tap to add friend"
 */
private class FriendsAdapter(
    private val onAddFriend: (UserProfile) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val items = mutableListOf<UserProfile>()

    fun submitList(users: List<UserProfile>) {
        items.clear()
        items.addAll(users)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(user: UserProfile) {
            text1.text = if (user.username.isNotBlank()) user.username else user.email
            text2.text = "Tap to add friend"

            itemView.setOnClickListener {
                onAddFriend(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
