package com.example.cs388finalproject.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cs388finalproject.databinding.ActivitySongDetailBinding

class SongDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySongDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySongDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val songName = intent.getStringExtra("songName") ?: "Unknown Song"
        val artistName = intent.getStringExtra("artistName") ?: "Unknown Artist"
        val albumName = intent.getStringExtra("albumName") ?: "Unknown Album"
        val albumArtUrl = intent.getStringExtra("albumArtUrl") ?: ""
        val durationMs = intent.getIntExtra("durationMs", 0)
        val explicit = intent.getBooleanExtra("explicit", false)

        binding.textSongTitle.text = songName
        binding.textArtistName.text = "By: $artistName"
        binding.textAlbumName.text = "Album: $albumName"

        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        binding.textDuration.text = "Duration: %d:%02d".format(minutes, seconds)

        binding.textExplicit.text = if (explicit) "Explicit: Yes" else "Explicit: No"

        Glide.with(this)
            .load(albumArtUrl)
            .into(binding.imageAlbumArt)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}