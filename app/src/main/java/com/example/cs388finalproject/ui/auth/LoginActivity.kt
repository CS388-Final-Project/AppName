package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.databinding.ActivityLoginBinding
import com.example.cs388finalproject.util.Validators
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }
        binding.tvToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val pw = binding.etPassword.text.toString()

        if (!Validators.isValidEmail(email)) return toast("Enter a valid email")
        if (pw.isEmpty()) return toast("Enter your password")

        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, pw)
            .addOnSuccessListener {
                toast("Welcome back!")
                // Navigate to main screen
                startActivity(Intent(this, /* MainActivity::class.java */ SignupActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                toast("Login failed: ${it.message}")
                binding.btnLogin.isEnabled = true
            }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
