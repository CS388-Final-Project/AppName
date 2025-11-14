package com.example.cs388finalproject.ui.home
import com.example.cs388finalproject.R

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {


    private lateinit var etUsername: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSaveChanges: Button
    private lateinit var btnBackArrow: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db  = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etUsername = findViewById(R.id.etUsername)
        tvEmail = findViewById(R.id.tvEmail)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnBackArrow = findViewById(R.id.btn_back_arrow)

        loadUsername()

        btnSaveChanges.setOnClickListener {
            saveUsername()
        }

        btnBackArrow.setOnClickListener {
            finish()
        }
    }

    private fun loadUsername() {

        val user = auth.currentUser
        if (user == null) {
            // If somehow not logged in, close the activity
            finish()
            return
        }

        tvEmail.text = user.email ?: "Email Not Available"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                etUsername.setText(username ?: "")
            }
            .addOnFailureListener {
                etUsername.setText("Error loading username!!!!")
            }
    }

    private fun saveUsername() {
        val newUsername = etUsername.text.toString().trim()
        val user = auth.currentUser

        if(newUsername.isBlank()) {
            etUsername.error = "Username cannot be empty"
            return
        }

        if (user == null) return

        db.collection("users").document(user.uid)
            .update("username", newUsername)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener {

            }
    }

}