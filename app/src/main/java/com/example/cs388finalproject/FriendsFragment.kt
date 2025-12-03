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
    private var isGuestSession: Boolean = false

    private fun isGuest(): Boolean {
        return auth.currentUser?.isAnonymous == true
    }

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

            currentUid = null
            return
        }

        // Logged in user
        isGuestSession = false
        currentUid = user.uid

        adapter = FriendsAdapter(
            onAddFriend = { profile ->
                addFriend(user.uid, profile.uid)
            },
            isActionAvailable = true
        )

        setupSearchUi()
        binding.tvFriendsHint.text = "Search for users to add as friends."

        binding.recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFriends.adapter = adapter

        loadInitialData(currentUid)
    }

    private fun setupSearchUi() {
        binding.rbModeUsers.isChecked = true
        binding.rbModeMusic.isChecked = false

        binding.rgSearchMode.setOnCheckedChangeListener { _, checkedId ->
            searchMode = if (checkedId == binding.rbModeMusic.id) SearchMode.MUSIC else SearchMode.USERS
            applySearch()
        }

        binding.etSearch.addTextChangedListener { text ->
            currentQuery = text?.toString().orEmpty()
            applySearch()
        }
    }

    private fun loadInitialData(currentUid: String?) {
        // Load ALL users
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
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

        // Load current user’s friends
        if (currentUid != null && !isGuestSession) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    val friends = (doc.get("friends") as? List<String>) ?: emptyList()
                    friendIds.clear()
                    friendIds.addAll(friends)
                    applySearch()
                }
        }

        // Load posts (for music mode)
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

    private fun applySearch() {
        val uid = currentUid
        val query = currentQuery.trim().lowercase()

        val results: List<UserProfile> = when (searchMode) {
            SearchMode.USERS -> {
                val candidates = allUsers.filter { u ->
                    if (uid != null && !isGuestSession) u.uid !in friendIds else true
                }

                if (query.isBlank()) candidates
                else candidates.filter { u ->
                    u.username.contains(query, true) ||
                            u.email.contains(query, true)
                }
            }

            SearchMode.MUSIC -> {
                if (query.isBlank()) emptyList()
                else {
                    val matchingUids = allPosts.filter { p ->
                        p.songName.contains(query, true) ||
                                p.artistName.contains(query, true) ||
                                p.albumName.contains(query, true)
                    }.map { it.uid }.distinct()

                    allUsers.filter { u ->
                        val isFriend = uid != null && !isGuestSession && u.uid in friendIds
                        !isFriend && u.uid in matchingUids
                    }
                }
            }
        }

        adapter.submitList(results)

        binding.tvFriendsHint.text = when {
            results.isEmpty() && query.isBlank() && searchMode == SearchMode.MUSIC ->
                "Type a song, artist, or album to find users."
            results.isEmpty() && query.isBlank() ->
                "Search for users to add as friends."
            results.isEmpty() ->
                "No results for \"$query\"."
            searchMode == SearchMode.USERS ->
                "User results for \"$query\""
            else ->
                "Users who posted music matching \"$query\""
        }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// -------------------------------------------------------------------------------------

private class FriendsAdapter(
    private val onAddFriend: (UserProfile) -> Unit,
    private val isActionAvailable: Boolean
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val items = mutableListOf<UserProfile>()

    fun submitList(users: List<UserProfile>) {
        items.clear()
        items.addAll(users)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val text1: TextView = itemView.findViewById(R.id.text1)
        private val text2: TextView = itemView.findViewById(R.id.text2)

        fun bind(user: UserProfile) {
            text1.text = if (user.username.isNotBlank()) user.username else user.email
            text2.text = if (isActionAvailable) "Tap to add friend" else "Tap to sign up"

            itemView.setOnClickListener { onAddFriend(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_list, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}