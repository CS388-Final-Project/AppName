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
import com.example.cs388finalproject.ui.MyPostsActivity
import com.example.cs388finalproject.ui.auth.GuestSession
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.ui.auth.SignupActivity
import com.example.cs388finalproject.ui.SongDetailActivity
import com.example.cs388finalproject.ui.home.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var friendIds: List<String> = emptyList()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) uploadImageToFirebase(imageUri)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) selectImageFromGallery()
            else Toast.makeText(requireContext(), "Permission needed", Toast.LENGTH_SHORT).show()
        }

    private fun isGuest(): Boolean {
        return GuestSession.isGuest(requireContext()) || auth.currentUser?.isAnonymous == true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.layoutTopTracks.visibility = View.GONE

        loadUserData()
        loadProfilePicture()

        binding.btnSettings.setOnClickListener {
            if (isGuest()) {
                Toast.makeText(requireContext(), "Sign Up to edit profile", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), SignupActivity::class.java))
            } else {
                startActivity(Intent(requireActivity(), SettingsActivity::class.java))
            }
        }

        binding.btnChangePhoto.setOnClickListener {
            if (isGuest()) {
                Toast.makeText(requireContext(), "Sign Up to edit profile", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), SignupActivity::class.java))
            } else {
                checkAndRequestPermission()
            }
        }

        binding.btnConnectSpotifyProfile.setOnClickListener {
            if (isGuest()) {
                startActivity(Intent(requireContext(), SignupActivity::class.java))
            } else {
                (activity as? MainActivity)?.startSpotifyLogin()
            }
        }

        binding.btnViewFriends.setOnClickListener { showFriendsDialog() }


        binding.btnViewMyPosts.setOnClickListener {
            openMyPostsScreen()
        }

        (activity as? MainActivity)?.getSpotifyState()?.let { state ->
            if (!isGuest()) updateSpotifyUi(state.profile, state.topTracks)
        }

        binding.btnLogout.setOnClickListener { handleLogout() }

        return binding.root
    }

    private fun handleLogout() {
        val user = auth.currentUser

        if (isGuest()) {
            if (user?.isAnonymous == true) {
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            GuestSession.clearAll(requireContext())
                            auth.signOut()
                            startActivity(Intent(requireActivity(), LoginActivity::class.java))
                            requireActivity().finish()
                            Toast.makeText(requireContext(), "Guest session ended.", Toast.LENGTH_SHORT).show()
                        } else {
                            GuestSession.clearAll(requireContext())
                            auth.signOut()
                            startActivity(Intent(requireActivity(), LoginActivity::class.java))
                            requireActivity().finish()
                            Toast.makeText(requireContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show()
                        }
                    }
                return
            } else {
                GuestSession.clearAll(requireContext())
            }
        }

        auth.signOut()
        startActivity(Intent(requireActivity(), LoginActivity::class.java))
        requireActivity().finish()
        Toast.makeText(requireContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadProfilePicture()
        applyGuestRestrictions()

        (activity as? MainActivity)?.getSpotifyState()?.let { state ->
            if (!isGuest()) updateSpotifyUi(state.profile, state.topTracks)
        }
    }

    private fun applyGuestRestrictions() {
        if (!isGuest()) {
            binding.btnLogout.visibility = View.GONE
            binding.btnConnectSpotifyProfile.isEnabled = true
            binding.btnConnectSpotifyProfile.alpha = 1f
            binding.btnChangePhoto.isEnabled = true
            binding.btnChangePhoto.alpha = 1f
            return
        }

        binding.tvUsername.text = "Guest User"
        binding.tvBio.text = "No Bio Yet"

        binding.btnLogout.visibility = View.VISIBLE
        binding.btnLogout.text = "Exit Guest / Log In"

        binding.btnChangePhoto.isEnabled = false
        binding.btnChangePhoto.alpha = 0.4f

        binding.btnConnectSpotifyProfile.text = "Sign Up to connect to Spotify"
        binding.btnConnectSpotifyProfile.alpha = 1f
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null || isGuest()) {
            binding.tvUsername.text = "Guest User"
            binding.tvBio.text = "No Bio Yet"
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                binding.tvUsername.text = doc.getString("username") ?: "N/A"
                val bio = doc.getString("bio") ?: "No Bio Yet"
                binding.tvBio.text = bio

                friendIds = (doc.get("friends") as? List<String>) ?: emptyList()
                updateFriendsButton()
            }
            .addOnFailureListener {
                binding.tvUsername.text = "N/A"
                binding.tvBio.text = "No Bio Yet"
            }
    }

    private fun updateFriendsButton() {
        val count = friendIds.size
        binding.btnViewFriends.text =
            if (count > 0) "View Friends ($count)" else "View Friends"
    }

    private fun showFriendsDialog() {
        if (!isAdded) return
        if (friendIds.isEmpty()) {
            Toast.makeText(requireContext(), "You haven't added any friends yet.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val idsForQuery = if (friendIds.size > 10) friendIds.take(10) else friendIds

        db.collection("users").whereIn("uid", idsForQuery).get()
            .addOnSuccessListener { snapshot ->
                val names = snapshot.documents.map { doc ->
                    doc.getString("username")
                        ?: doc.getString("email")
                        ?: "Unknown user"
                }.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Your Friends")
                    .setItems(names) { _, which ->
                        val uidToRemove = idsForQuery[which]
                        val name = names[which]

                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Remove friend?")
                            .setMessage("Remove $name from your friends?")
                            .setPositiveButton("Remove") { _, _ -> removeFriend(uidToRemove) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load friends.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFriend(friendUid: String) {
        val currentUid = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUid)
            .update("friends", FieldValue.arrayRemove(friendUid))
            .addOnSuccessListener {
                friendIds = friendIds.filterNot { it == friendUid }
                updateFriendsButton()
                Toast.makeText(requireContext(), "Friend removed.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to remove friend.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun checkAndRequestPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_IMAGES
            else android.Manifest.permission.READ_EXTERNAL_STORAGE

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
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfilePictureUrl(url: String) {
        val id = auth.currentUser?.uid ?: return

        db.collection("users").document(id)
            .update("profilePictureUrl", url)
            .addOnSuccessListener { loadProfilePicture() }
            .addOnFailureListener {
                db.collection("users").document(id)
                    .set(mapOf("profilePictureUrl" to url), SetOptions.merge())
                    .addOnSuccessListener { loadProfilePicture() }
            }
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
            .addOnFailureListener {
                binding.imgProfilePhoto.setImageResource(R.drawable.outline_account_circle_24)
            }
    }

    fun updateSpotifyUi(
        profile: SpotifyProfile,
        tracks: List<SpotifyTrack>
    ) {
        if (isGuest()) return

        binding.tvSpotifyName.text = "Spotify: ${profile.displayName}"

        if (!profile.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.imageUrl)
                .circleCrop()
                .into(binding.imgSpotifyAvatar)
        }

        val t = tracks.take(3)

        fun apply(i: Int, img: android.widget.ImageView, tv: android.widget.TextView) {
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

        binding.imgTrack1.setOnClickListener { if (t.isNotEmpty()) launchSongDetails(t[0]) }
        binding.tvTrack1.setOnClickListener { if (t.isNotEmpty()) launchSongDetails(t[0]) }
        binding.imgTrack2.setOnClickListener { if (t.size > 1) launchSongDetails(t[1]) }
        binding.tvTrack2.setOnClickListener { if (t.size > 1) launchSongDetails(t[1]) }
        binding.imgTrack3.setOnClickListener { if (t.size > 2) launchSongDetails(t[2]) }
        binding.tvTrack3.setOnClickListener { if (t.size > 2) launchSongDetails(t[2]) }
    }

    // ⬇️ NEW: open "My Posts" activity
    private fun openMyPostsScreen() {
        val user = auth.currentUser
        if (user == null || isGuest()) {
            Toast.makeText(requireContext(), "You must be logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(requireContext(), MyPostsActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
