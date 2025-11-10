package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.databinding.ActivitySignupBinding
import com.example.cs388finalproject.model.UserProfile
import com.example.cs388finalproject.util.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener { submit() }
        binding.tvToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun submit() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pw = binding.etPassword.text.toString()
        val pw2 = binding.etConfirm.text.toString()

        if (username.isEmpty()) return toast("Username is required")
        if (!Validators.isValidEmail(email)) return toast("Enter a valid email")
        if (pw != pw2) return toast("Passwords do not match")
        if (!Validators.isStrongPassword(pw)) {
            return toast("Password must be 8+ chars with upper, lower, digit, special")
        }

        binding.btnSignup.isEnabled = false

        auth.createUserWithEmailAndPassword(email, pw)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener toast("No UID?")
                val profile = UserProfile(uid = uid, email = email, username = username)
                db.collection("users").document(uid).set(profile)
                    .addOnSuccessListener {
                        toast("Account created!")
                        // go to main/home screen
                        startActivity(Intent(this, /* MainActivity::class.java */ LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        toast("Saved auth, but failed to save profile: ${it.message}")
                        binding.btnSignup.isEnabled = true
                    }
            }
            .addOnFailureListener {
                toast("Signup failed: ${it.message}")
                binding.btnSignup.isEnabled = true
            }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
