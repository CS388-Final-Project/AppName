package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cs388finalproject.databinding.FragmentRecommendationsListBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.ui.SongDetailActivity
import com.google.firebase.firestore.FirebaseFirestore

class RecommendationsListFragment : Fragment() {

    private var _binding: FragmentRecommendationsListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: RecommendationsListAdapter
    private var title: String = ""
    private var items: Array<String> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = RecommendationsListFragmentArgs.fromBundle(requireArguments())
        title = args.title
        items = args.items
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvListTitle.text = title

        adapter = RecommendationsListAdapter { songName ->
            openSongDetails(songName)
        }

        binding.recyclerRecommendations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecommendations.adapter = adapter

        binding.btnBackList.setOnClickListener {
            findNavController().popBackStack()
        }

        loadSongRows()
    }

    private fun loadSongRows() {

        if (title == "Top Artists") {
            // ARTIST LIST MODE
            loadArtistRows()
            return
        }

        // SONG LIST MODE
        db.collection("posts")
            .whereIn("songName", items.toList())
            .get()
            .addOnSuccessListener { snapshot ->

                val posts = snapshot.toObjects(Post::class.java)

                // GROUP BY SONG NAME
                val grouped = posts.groupBy { it.songName }

                // BUILD RANKED LIST
                val ranked = grouped.map { (songName, postsForSong) ->
                    RecommendationRow(
                        rank = postsForSong.size, // temporary rank, will sort next
                        title = songName,
                        artist = postsForSong.first().artistName,
                        coverUrl = postsForSong.first().albumArtUrl ?: ""
                    )
                }
                    .sortedByDescending { it.rank }  // highest play count first
                    .mapIndexed { index, row -> row.copy(rank = index + 1) } // assign 1,2,3…

                adapter.submitList(ranked)
            }
    }

    private fun loadArtistRows() {
        db.collection("posts")
            .whereIn("artistName", items.toList())
            .get()
            .addOnSuccessListener { snapshot ->

                val posts = snapshot.toObjects(Post::class.java)

                val grouped = posts.groupBy { it.artistName }

                val ranked = grouped.map { (artistName, postsForArtist) ->
                    RecommendationRow(
                        rank = postsForArtist.size,
                        title = artistName,
                        artist = "",
                        coverUrl = postsForArtist.first().albumArtUrl ?: ""
                    )
                }
                    .sortedByDescending { it.rank }
                    .mapIndexed { index, row -> row.copy(rank = index + 1) }

                adapter.submitList(ranked)
            }
    }

    private fun openSongDetails(songName: String) {
        db.collection("posts")
            .whereEqualTo("songName", songName)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val post = snapshot.toObjects(Post::class.java).firstOrNull() ?: return@addOnSuccessListener

                val intent = Intent(requireContext(), SongDetailActivity::class.java).apply {
                    putExtra("songName", post.songName)
                    putExtra("artistName", post.artistName)
                    putExtra("albumName", post.albumName ?: "")
                    putExtra("albumArtUrl", post.albumArtUrl ?: "")
                    putExtra("durationMs", post.durationMs)
                    putExtra("explicit", post.explicit)
                }

                startActivity(intent)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}