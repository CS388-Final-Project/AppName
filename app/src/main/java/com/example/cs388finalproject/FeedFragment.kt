package com.example.cs388finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cs388finalproject.data.PostRepository
import com.example.cs388finalproject.databinding.FragmentFeedBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.ui.home.FeedAdapter
import com.google.firebase.firestore.ktx.toObjects

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val repo = PostRepository()
    private val adapter = FeedAdapter()

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

        binding.recyclerFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFeed.adapter = adapter

        // Listen for live updates to the feed
        repo.feed().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                // TODO: show error UI if you want
                return@addSnapshotListener
            }
            val posts = snapshot.toObjects<Post>()
            adapter.submitList(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
