package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.MainActivity
import com.example.cs388finalproject.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (email.isEmpty() || username.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isStrong(pass)) {
                Toast.makeText(this, "Weak password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Signing up -> disable guest session
            GuestSession.setGuest(this, false)
            GuestSession.setFirstLaunchDone(this)

            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = hashMapOf(
                        "uid" to uid,
                        "email" to email,
                        "username" to username,
                        "isGuest" to false
                    )
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            // go to main / or login - we'll go to MainActivity so user is logged in
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Browse as Guest from signup screen too
        binding.guestButton.setOnClickListener {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    GuestSession.setGuest(this, true)
                    GuestSession.setFirstLaunchDone(this)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Guest sign-in failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun isStrong(p: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}\$")
        return regex.matches(p)
    }
}
