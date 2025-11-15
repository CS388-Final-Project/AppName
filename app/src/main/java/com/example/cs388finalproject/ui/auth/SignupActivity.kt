package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            val email = binding.etEmail.text.toString()
            val username = binding.etUsername.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (pass != confirm) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isStrong(pass)) {
                Toast.makeText(this, "Weak password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                if (it.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = hashMapOf(
                        "uid" to uid,
                        "email" to email,
                        "username" to username
                    )
                    db.collection("users").document(uid).set(user)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun isStrong(p: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}\$")
        return regex.matches(p)
    }
}
