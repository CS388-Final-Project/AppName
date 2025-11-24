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

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val repo = PostRepository()
    private lateinit var adapter: FeedAdapter

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

        repo.feed().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val posts = snapshot.toObjects<Post>()
            adapter.submitList(posts)
        }

        binding.buttonCreatePost.setOnClickListener {
            val main = requireActivity() as MainActivity
            if (main.isGuestUser()) {
                Toast.makeText(requireContext(), "Sign Up to create a post", Toast.LENGTH_SHORT).show()
                // optionally navigate to signup:
                startActivity(Intent(requireContext(), com.example.cs388finalproject.ui.auth.SignupActivity::class.java))
                return@setOnClickListener
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
