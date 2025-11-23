package com.example.cs388finalproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.cs388finalproject.databinding.FragmentProfileBinding
import com.example.cs388finalproject.ui.home.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.cs388finalproject.ui.SongDetailActivity


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) uploadImageToFirebase(imageUri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) selectImageFromGallery()
        else Toast.makeText(requireContext(), "Permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserData()
        loadProfilePicture()

        // Hide Spotify UI until connected
        binding.layoutTopTracks.visibility = View.GONE

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireActivity(), SettingsActivity::class.java))
        }

        binding.btnChangePhoto.setOnClickListener { checkAndRequestPermission() }

        binding.btnConnectSpotifyProfile.setOnClickListener {
            (activity as? MainActivity)?.startSpotifyLogin()
        }

        // ---- FIX: Use topTracks instead of tracks ----
        (activity as? MainActivity)?.getSpotifyState()?.let { state ->
            updateSpotifyUi(state.profile, state.topTracks, animate = false)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadProfilePicture()

        (activity as? MainActivity)?.getSpotifyState()?.let { state ->
            updateSpotifyUi(state.profile, state.topTracks, animate = false)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        binding.tvEmail.text = user.email ?: "Email Not Available"

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener {
                binding.tvUsername.text = it.getString("username") ?: "N/A"
            }
    }

    private fun checkAndRequestPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_IMAGES
            else
                android.Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            requireContext().checkSelfPermission(permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED ->
                selectImageFromGallery()

            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val id = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("profile_pictures/$id")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { download ->
                    saveProfilePictureUrl(download.toString())
                }
            }
    }

    private fun saveProfilePictureUrl(url: String) {
        val id = auth.currentUser?.uid ?: return
        db.collection("users").document(id)
            .update("profilePictureUrl", url)
            .addOnSuccessListener { loadProfilePicture() }
    }

    private fun loadProfilePicture() {
        val id = auth.currentUser?.uid ?: return

        db.collection("users").document(id).get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("profilePictureUrl")

                if (!url.isNullOrEmpty()) {
                    Glide.with(this).load(url).circleCrop().into(binding.imgProfilePhoto)
                } else {
                    binding.imgProfilePhoto.setImageResource(R.drawable.outline_account_circle_24)
                }
            }
    }

    // ---- FIXED VERSION: Uses topTracks ----
    fun updateSpotifyUi(
        profile: SpotifyProfile,
        tracks: List<SpotifyTrack>,
        animate: Boolean = true
    ) {
        binding.tvSpotifyName.text = "Spotify: ${profile.displayName}"

        if (!profile.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.imageUrl)
                .circleCrop()
                .into(binding.imgSpotifyAvatar)
        }

        val t = tracks.take(3)

        fun apply(
            i: Int,
            img: android.widget.ImageView,
            tv: android.widget.TextView
        ) {
            if (i >= t.size) {
                tv.text = ""
                img.setImageDrawable(null)
                return
            }
            tv.text = "${t[i].name}\n— ${t[i].artist}"
            Glide.with(this).load(t[i].imageUrl).centerCrop().into(img)
        }

        apply(0, binding.imgTrack1, binding.tvTrack1)
        apply(1, binding.imgTrack2, binding.tvTrack2)
        apply(2, binding.imgTrack3, binding.tvTrack3)

        binding.btnConnectSpotifyProfile.visibility = View.GONE
        binding.layoutTopTracks.visibility = View.VISIBLE

        // ─────────────────────────────────────────────
        // MAKE TOP 3 TRACKS CLICKABLE
        // ─────────────────────────────────────────────
        fun launchSongDetails(track: SpotifyTrack) {
            val intent = Intent(requireContext(), SongDetailActivity::class.java)
            intent.putExtra("songName", track.name)
            intent.putExtra("artistName", track.artist)
            intent.putExtra("albumName", track.albumName)
            intent.putExtra("albumArtUrl", track.imageUrl)
            intent.putExtra("durationMs", track.durationMs)
            intent.putExtra("explicit", track.explicit)
            intent.putExtra("previewUrl", track.previewUrl ?: "")
            startActivity(intent)
        }

        // Track 1
        binding.imgTrack1.setOnClickListener { if (t.isNotEmpty()) launchSongDetails(t[0]) }
        binding.tvTrack1.setOnClickListener { if (t.isNotEmpty()) launchSongDetails(t[0]) }

        // Track 2
        binding.imgTrack2.setOnClickListener { if (t.size > 1) launchSongDetails(t[1]) }
        binding.tvTrack2.setOnClickListener { if (t.size > 1) launchSongDetails(t[1]) }

        // Track 3
        binding.imgTrack3.setOnClickListener { if (t.size > 2) launchSongDetails(t[2]) }
        binding.tvTrack3.setOnClickListener { if (t.size > 2) launchSongDetails(t[2]) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}