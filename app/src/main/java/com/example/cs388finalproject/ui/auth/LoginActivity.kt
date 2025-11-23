package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.MainActivity
import com.example.cs388finalproject.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Keep import in case other functions need it

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // Still initialized but not used in doLogin()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // Initialized

        binding.btnLogin.setOnClickListener { doLogin() }

        binding.tvToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.btnLoginGuest.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.tvToForgotPass.setOnClickListener {
            //add forgot pass stuff here
            val email = binding.etUsername.text.toString().trim()

            if (email.isEmpty()) {
                toast("Enter Your Email To Reset Your Password")
                return@setOnClickListener
            }
            setLoading(true)

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    setLoading(false)
                    toast("Password reset email sent")
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    toast("error: ${e.message}")
                }
        }

    }

    private fun doLogin() {
        // --- KEY CHANGE 1: Renamed variable to email ---
        val email = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            toast("Enter email and password")
            return
        }

        setLoading(true)

        // --- KEY CHANGE 2: Removed Firestore query and call Firebase Auth directly ---
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
                // Firebase Auth automatically provides helpful errors, "Wrong credentials" covers invalid email or password.
                toast("Wrong credentials")
            }
        // --- End KEY CHANGE 2 ---
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}