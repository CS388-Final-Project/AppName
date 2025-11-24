package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cs388finalproject.data.PostRepository
import com.example.cs388finalproject.databinding.FragmentFeedBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.ui.CreatePostActivity
import com.example.cs388finalproject.ui.SongDetailActivity
import com.example.cs388finalproject.ui.home.FeedAdapter
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val repo = PostRepository()
    private lateinit var adapter: FeedAdapter

    private var feedListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FeedAdapter { post -> openSongDetails(post) }

        binding.recyclerFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFeed.adapter = adapter

        setupFeed()

        // Live updates
        repo.feed().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val posts = snapshot.toObjects<Post>()
            adapter.submitList(posts)
        }

        binding.buttonCreatePost.setOnClickListener {
            startCreatePostFlow()
        }
    }

    private fun startCreatePostFlow() {
        val main = requireActivity() as MainActivity
        val state = main.getSpotifyState()
        val current = state?.currentTrack

        if (current == null) {
            Toast.makeText(
                requireContext(),
                "No song is currently playing on Spotify.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = Intent(requireContext(), CreatePostActivity::class.java).apply {
            putExtra("songId", current.id)
            putExtra("songName", current.name)
            putExtra("artistName", current.artist ?: "")
            putExtra("albumName", current.albumName ?: "")
            putExtra("albumArtUrl", current.imageUrl ?: "")
            putExtra("durationMs", current.durationMs)
            putExtra("explicit", current.explicit)
            putExtra("previewUrl", current.previewUrl ?: "")
        }

        startActivity(intent)
    }

    private fun openSongDetails(post: Post) {
        val intent = Intent(requireContext(), SongDetailActivity::class.java).apply {
            putExtra("songName", post.songName)
            putExtra("artistName", post.artistName)
            putExtra("albumName", post.albumName)
            putExtra("albumArtUrl", post.albumArtUrl)
            putExtra("durationMs", post.durationMs)
            putExtra("explicit", post.explicit)
        }
        startActivity(intent)
    }

    private fun setupFeed() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Guest → global feed
            listenToGlobalFeed()
        } else {
            // Logged in → only friends
            listenToFriendsFeed(currentUser.uid)
        }
    }

    /** Global feed → all posts, newest first  */
    private fun listenToGlobalFeed() {
        feedListener?.remove()
        feedListener = repo.feed().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val posts = snapshot.toObjects<Post>()
            adapter.submitList(posts)
        }
    }

    /**
     * Friends-only feed: this is so logged in users see the feed comprised of their
     * friends posts and guest users should see a global feed
     */
    private fun listenToFriendsFeed(currentUid: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUid)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) {
                    // If anything goes wrong, fall back to global feed
                    listenToGlobalFeed()
                    return@addSnapshotListener
                }

                val friends = (doc.get("friends") as? List<String>) ?: emptyList()
                val friendUids = (friends + currentUid).distinct()

                feedListener?.remove()

                if (friendUids.isEmpty()) {
                    adapter.submitList(emptyList())
                    return@addSnapshotListener
                }

                // Firestore whereIn supports up to 10 values; for more you'd split the query.
                val limitedIds = if (friendUids.size > 10) friendUids.take(10) else friendUids

                feedListener = db.collection("posts")
                    .whereIn("uid", limitedIds)
                    .addSnapshotListener { snapshot, postsError ->
                        if (postsError != null || snapshot == null) {
                            return@addSnapshotListener
                        }
                        val posts = snapshot.toObjects<Post>()
                            .sortedByDescending { it.createdAt }

                        adapter.submitList(posts)
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}