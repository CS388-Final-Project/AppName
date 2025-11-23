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
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.ui.home.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
            if (imageUri != null) {
                uploadImageToFirebase(imageUri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            selectImageFromGallery()
        } else {
            Toast.makeText(requireContext(), "Permission is required to set profile picture.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        val user = auth.currentUser
        val isGuest = user == null

        binding.btnLogout.text = if (isGuest) "Sign In" else "Log Out"

        binding.btnLogout.setOnClickListener {
            if (isGuest) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
            else {
                auth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        binding.btnSettings.setOnClickListener {
            if(isGuest) {
                Toast.makeText(requireContext(), "Sign in to edit profile", Toast.LENGTH_SHORT).show()
            }
            else{
                startActivity(Intent(requireActivity(), SettingsActivity::class.java))
            }
        }
        binding.btnChangePhoto.setOnClickListener {
            if(isGuest) {
                Toast.makeText(requireContext(), "Sign in to change photo", Toast.LENGTH_SHORT).show()
            }
            else {
                checkAndRequestPermission()
            }
        }

        if (!isGuest) {
            loadUserData()
            loadProfilePicture()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(auth.currentUser != null) {
            loadUserData()
            loadProfilePicture()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        binding.tvEmail.text = user.email ?: "Email Not Available"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                binding.tvUsername.text = document.getString("username") ?: "N/A"
            }
            .addOnFailureListener {
                binding.tvUsername.text = "Error Loading Username"
            }
    }

    private fun checkAndRequestPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                // Android 12 and below
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

        when {
            // Permission is already granted
            requireContext().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                selectImageFromGallery()
            }
            // Request the permission
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        // Storage location: /profile_pictures/{userId}.jpg
        val storageRef = storage.reference.child("profile_pictures/$userId")

        // Start the upload task
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Get the download URL
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    saveProfilePictureUrl(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveProfilePictureUrl(url: String) {
        val userId = auth.currentUser?.uid ?: return

        // Save the URL to Firestore
        db.collection("users").document(userId)
            .update("profilePictureUrl", url)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Photo updated!", Toast.LENGTH_SHORT).show()
                loadProfilePicture() // Reload the new image
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save photo URL.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfilePicture() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val url = document.getString("profilePictureUrl")

            // Use Glide to handle image loading and circular cropping
            val glideBuilder = Glide.with(this).load(url)

            if (!url.isNullOrEmpty()) {
                glideBuilder
                    .circleCrop()
                    .into(binding.imgProfilePhoto)
            } else {
                // Load a default placeholder image if no URL is found
                binding.imgProfilePhoto.setImageResource(R.drawable.outline_account_circle_24)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}