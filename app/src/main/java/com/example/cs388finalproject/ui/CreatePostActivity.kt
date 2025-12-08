package com.example.cs388finalproject.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.cs388finalproject.R
import com.example.cs388finalproject.databinding.ActivityCreatePostBinding
import com.example.cs388finalproject.model.Post
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var imageUri: Uri? = null



    // For Gallery Selection
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            if (imageUri != null) loadPhotoPreview()
        }
    }

    // For Camera Capture
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // imageUri is already set to the temporary file URI before launching the camera
            if (imageUri != null) loadPhotoPreview()
        }
    }

    // For Requesting Permissions (Camera/Storage)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if both camera and storage permissions were granted
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        } else true

        if (cameraGranted && storageGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Camera/Storage permissions denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- ON CREATE & SETUP ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initial setup for the photo preview
        binding.imgPostPhoto.setImageResource(R.drawable.sharp_add_photo_alternate_24)

        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Upload button
        binding.btnUpload.setOnClickListener { createPost() }

        // Select Photo Button
        binding.btnSelectPhoto.setOnClickListener {
            requestPermissions()
        }
    }

    // --- PHOTO HANDLING ---

    private fun requestPermissions() {
        val permissions = mutableListOf(android.Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showImageSourceDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> launchCamera()
                options[item] == "Choose from Gallery" -> launchGallery()
                options[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Create a temporary file to store the image
        val photoFile: File? = try {
            createImageFile()
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating photo file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.let {
            // Get the content URI using FileProvider
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            imageUri = photoURI // Set the URI for the camera result
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cameraLauncher.launch(cameraIntent)
        }
    }

    // Helper function to create a temporary image file
    private fun createImageFile(): File {
        val timeStamp: String = System.currentTimeMillis().toString()
        val storageDir: File? = externalCacheDir // Use external cache for temp file
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun launchGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    private fun loadPhotoPreview() {
        if (imageUri != null) {
            Glide.with(this).load(imageUri).centerCrop().into(binding.imgPostPhoto)
        }
    }

    //  POST CREATION & UPLOAD

    private fun uploadImageAndCreatePost(userUid: String, songMetadata: Map<String, Any>, location: Map<String, Double>) {

        // Check if imageUri is set, if not, use the placeholder and proceed
        val imageToUpload = imageUri
        if (imageToUpload == null) {
            val placeholder = "https://picsum.photos/600/600"
            writePostToFirestore(userUid, songMetadata, location, placeholder)
            return
        }

        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()
        binding.btnUpload.isEnabled = false

        // Upload to Firebase Storage
        val storageRef = storage.reference.child("post_images/${userUid}/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(imageToUpload)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Image uploaded, now save the post
                    writePostToFirestore(userUid, songMetadata, location, downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                binding.btnUpload.isEnabled = true
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    //Storing Posts to the Database
    private fun writePostToFirestore(
        userUid: String,
        songMetadata: Map<String, Any>,
        location: Map<String, Double>,
        imageUrl: String
    ) {

        val now = System.currentTimeMillis()

        db.collection("users").document(userUid).get()
            .addOnSuccessListener { userDoc ->

                val username = userDoc.getString("username") ?: "Unknown"

                // Always create a new unique document
                val postsCollection = db.collection("posts")
                val docRef = postsCollection.document() // Generate unique ID

                val postId = docRef.id

                val newPost = Post(
                    postId = postId,
                    uid = userUid,
                    username = username,
                    imagePath = imageUrl,
                    songId = songMetadata["songId"] as String,
                    songName = songMetadata["songName"] as String,
                    artistName = songMetadata["artistName"] as String,
                    albumName = songMetadata["albumName"] as String,
                    albumArtUrl = songMetadata["albumArtUrl"] as String,
                    durationMs = songMetadata["durationMs"] as Int,
                    explicit = songMetadata["explicit"] as Boolean,
                    previewUrl = songMetadata["previewUrl"] as String,
                    location = location,
                    createdAt = now
                )

                docRef.set(newPost)
                    .addOnSuccessListener {
                        binding.btnUpload.isEnabled = true
                        Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.btnUpload.isEnabled = true
                        Toast.makeText(this, "Post failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun createPost() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "You must be logged in to post.", Toast.LENGTH_SHORT).show()
            return
        }

        //  Get Song data
        val songMetadata = mapOf(
            "songId" to (intent.getStringExtra("songId") ?: ""),
            "songName" to (intent.getStringExtra("songName") ?: "Unknown Song"),
            "artistName" to (intent.getStringExtra("artistName") ?: "Unknown Artist"),
            "albumName" to (intent.getStringExtra("albumName") ?: "Unknown Album"),
            "albumArtUrl" to (intent.getStringExtra("albumArtUrl") ?: ""),
            "durationMs" to intent.getIntExtra("durationMs", 0),
            "explicit" to intent.getBooleanExtra("explicit", false),
            "previewUrl" to (intent.getStringExtra("previewUrl") ?: "")
        )

        //  Fetch Location first
        getCurrentLatLng { lat, lng ->
            val location = mapOf("lat" to lat, "lng" to lng)

            // Simply upload the post without checking for window logic
            uploadImageAndCreatePost(user.uid, songMetadata, location)
        }
    }

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
}