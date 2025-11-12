package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.MainActivity
import com.example.cs388finalproject.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnLogin.setOnClickListener { doLogin() }

        binding.tvToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    private fun doLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            toast("Enter username and password")
            return
        }

        setLoading(true)

        // Find email by username
        db.collection("users").whereEqualTo("username", username).limit(1).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    setLoading(false)
                    toast("User not found")
                    return@addOnSuccessListener
                }
                val email = snap.documents.first().getString("email") ?: ""
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        setLoading(false)
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra("fromLogin", true)
                        )
                        finish()
                    }
                    .addOnFailureListener {
                        setLoading(false)
                        toast("Wrong credentials")
                    }
            }
            .addOnFailureListener {
                setLoading(false)
                toast("Login failed")
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
