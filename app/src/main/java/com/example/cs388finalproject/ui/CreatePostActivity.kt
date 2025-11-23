package com.example.cs388finalproject.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.databinding.ActivityCreatePostBinding
import com.example.cs388finalproject.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.LocationServices

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Upload button
        binding.btnUpload.setOnClickListener {
            createPost()
        }
    }

    // Get GPS location safely (no MainActivity dependency)
    private fun getCurrentLatLng(callback: (Double, Double) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(this)

        try {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        callback(loc.latitude, loc.longitude)
                    } else {
                        callback(0.0, 0.0)
                    }
                }
                .addOnFailureListener {
                    callback(0.0, 0.0)
                }
        } catch (e: SecurityException) {
            callback(0.0, 0.0)
        }
    }

    private fun createPost() {
        val user = auth.currentUser ?: return

        // Spotify metadata from intent
        val songId = intent.getStringExtra("songId") ?: ""
        val songName = intent.getStringExtra("songName") ?: "Unknown Song"
        val artistName = intent.getStringExtra("artistName") ?: "Unknown Artist"
        val albumName = intent.getStringExtra("albumName") ?: "Unknown Album"
        val albumArtUrl = intent.getStringExtra("albumArtUrl") ?: ""
        val durationMs = intent.getIntExtra("durationMs", 0)
        val explicit = intent.getBooleanExtra("explicit", false)
        val previewUrl = intent.getStringExtra("previewUrl") ?: ""

        // Placeholder image until gallery/camera is added
        val placeholderImage = "https://picsum.photos/600/600"

        // Fetch location then write post
        getCurrentLatLng { lat, lng ->
            val location = mapOf("lat" to lat, "lng" to lng)

            // Fetch username from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { userDoc ->

                    val username = userDoc.getString("username") ?: "Unknown"

                    val newPost = Post(
                        uid = user.uid,
                        username = username,
                        imagePath = placeholderImage,
                        songId = songId,
                        songName = songName,
                        artistName = artistName,
                        albumName = albumName,
                        albumArtUrl = albumArtUrl,
                        durationMs = durationMs,
                        explicit = explicit,
                        previewUrl = previewUrl,
                        location = location,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("posts")
                        .add(newPost)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }
}