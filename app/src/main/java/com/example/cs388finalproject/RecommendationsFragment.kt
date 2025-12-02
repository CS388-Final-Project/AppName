package com.example.cs388finalproject

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cs388finalproject.databinding.FragmentRecommendationsBinding
import com.example.cs388finalproject.model.Post
import com.example.cs388finalproject.ui.auth.GuestSession
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class RecommendationsFragment : Fragment() {

    private var _binding: FragmentRecommendationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isGuest = GuestSession.isGuest(requireContext())
        val user = auth.currentUser

        if (isGuest || user == null || user.isAnonymous)
            loadGuestUi()
        else
            loadSignedInUi(user.uid)
    }

    // ---------------------------------------------------------
    // Helper: Ellipsize Long Text
    // ---------------------------------------------------------
    private fun ellipsize(text: String, maxLength: Int = 30): String {
        return if (text.length > maxLength) text.take(maxLength) + "…" else text
    }

    // ---------------------------------------------------------
    // Guest Mode UI
    // ---------------------------------------------------------
    private fun loadGuestUi() {
        binding.tvTopSongsTitle.text = "Top Songs on MusicMedia"
        binding.tvTopArtistsTitle.text = "Top Artists on MusicMedia"

        binding.cardTopLocation.visibility = View.GONE

        db.collection("posts").get()
            .addOnSuccessListener { snap ->
                val posts = snap.toObjects(Post::class.java)

                // Top 5 for preview
                val topSongs = posts.groupingBy { it.songName }.eachCount()
                    .toList().sortedByDescending { it.second }.take(10)

                val topArtists = posts.groupingBy { it.artistName }.eachCount()
                    .toList().sortedByDescending { it.second }.take(10)

                binding.tvTopSongsSummary.text =
                    topSongs.mapIndexed { i, p -> "${i + 1}) ${ellipsize(p.first)}" }
                        .joinToString("\n")

                binding.tvTopArtistsSummary.text =
                    topArtists.mapIndexed { i, p -> "${i + 1}) ${ellipsize(p.first)}" }
                        .joinToString("\n")

                binding.cardTopSongs.setOnClickListener {
                    openList("Top Songs on MusicMedia", posts.map { it.songName }.distinct().take(30))
                }

                binding.cardTopArtists.setOnClickListener {
                    openList("Top Artists on MusicMedia", posts.map { it.artistName }.distinct().take(30))
                }
            }
    }

    // ---------------------------------------------------------
    // Signed-In Mode
    // ---------------------------------------------------------
    private fun loadSignedInUi(uid: String) {

        binding.tvTopSongsTitle.text = "Top Songs from Your Circle"
        binding.tvTopArtistsTitle.text = "Top Artists from Your Circle"

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val friends = (doc.get("friends") as? List<String>) ?: emptyList()
                val circle = friends + uid

                db.collection("posts")
                    .whereIn("uid", circle)
                    .get()
                    .addOnSuccessListener { snap ->
                        val posts = snap.toObjects(Post::class.java)

                        val topSongs = posts.groupingBy { it.songName }.eachCount()
                            .toList().sortedByDescending { it.second }.take(5)

                        val topArtists = posts.groupingBy { it.artistName }.eachCount()
                            .toList().sortedByDescending { it.second }.take(5)

                        binding.tvTopSongsSummary.text =
                            topSongs.mapIndexed { i, p -> "${i + 1}) ${ellipsize(p.first)}" }
                                .joinToString("\n")

                        binding.tvTopArtistsSummary.text =
                            topArtists.mapIndexed { i, p -> "${i + 1}) ${ellipsize(p.first)}" }
                                .joinToString("\n")

                        binding.cardTopSongs.setOnClickListener {
                            openList(
                                "Top Songs from Your Circle",
                                posts.map { it.songName }.distinct().take(30)
                            )
                        }

                        binding.cardTopArtists.setOnClickListener {
                            openList(
                                "Top Artists from Your Circle",
                                posts.map { it.artistName }.distinct().take(30)
                            )
                        }
                    }

                loadLocationTopSongs()
            }
    }

    // ---------------------------------------------------------
    // Location-based Songs
    // ---------------------------------------------------------
    @SuppressLint("MissingPermission")
    private fun loadLocationTopSongs() {

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                binding.tvTopLocationSummary.text = "Couldn't get your location."
                return@addOnSuccessListener
            }

            val geo = Geocoder(requireContext(), Locale.getDefault())
            val addr = geo.getFromLocation(loc.latitude, loc.longitude, 1)
            val city = addr?.firstOrNull()?.locality ?: return@addOnSuccessListener
            val state = addr.firstOrNull()?.adminArea ?: ""

            binding.tvTopLocationTitle.text = "Top Songs in $city, $state"

            db.collection("posts").get()
                .addOnSuccessListener { snap ->
                    val posts = snap.toObjects(Post::class.java)

                    val localPosts = posts.filter { p ->
                        val plat = p.location["lat"] ?: return@filter false
                        val plng = p.location["lng"] ?: return@filter false

                        val a = geo.getFromLocation(plat, plng, 1)
                        val postCity = a?.firstOrNull()?.locality

                        postCity == city
                    }

                    val top = localPosts.groupingBy { it.songName }.eachCount()
                        .toList().sortedByDescending { it.second }.take(5)

                    binding.cardTopLocation.visibility = View.VISIBLE

                    binding.tvTopLocationSummary.text =
                        if (top.isEmpty()) "No posts near you."
                        else top.mapIndexed { i, p -> "${i + 1}) ${ellipsize(p.first)}" }
                            .joinToString("\n")

                    binding.cardTopLocation.setOnClickListener {
                        openList("Top Songs in $city, $state", localPosts.map { it.songName }.take(30))
                    }
                }
        }
    }

    // ---------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------
    private fun openList(title: String, items: List<String>) {
        val args = Bundle().apply {
            putString("title", title)
            putStringArray("items", items.toTypedArray())
        }
        findNavController().navigate(R.id.recommendationsListFragment, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}