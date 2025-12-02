package com.example.cs388finalproject.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.R
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etBio: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSaveChanges: Button
    private lateinit var btnBackArrow: ImageView
    private lateinit var btnLogout: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etUsername = findViewById(R.id.etUsername)
        etBio = findViewById(R.id.etBio)
        tvEmail = findViewById(R.id.tvEmail)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnBackArrow = findViewById(R.id.btn_back_arrow)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserData()

        btnSaveChanges.setOnClickListener { saveUserChanges() }

        btnBackArrow.setOnClickListener { finish() }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        tvEmail.text = user.email ?: "Email Not Available"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                etUsername.setText(document.getString("username") ?: "")
                etBio.setText(document.getString("bio") ?: "")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserChanges() {
        val newUsername = etUsername.text.toString().trim()
        val newBio = etBio.text.toString().trim()

        val user = auth.currentUser ?: return

        // VALIDATION
        if (newUsername.isBlank()) {
            etUsername.error = "Username cannot be empty"
            return
        }

        if (newBio.length > 100) {
            etBio.error = "Bio must be under 100 characters"
            return
        }

        val updates = mapOf(
            "username" to newUsername,
            "bio" to newBio
        )

        db.collection("users").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
            }
    }
}
