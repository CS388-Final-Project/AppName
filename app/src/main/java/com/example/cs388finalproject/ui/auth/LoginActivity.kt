package com.example.cs388finalproject.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cs388finalproject.MainActivity
import com.example.cs388finalproject.databinding.ActivityLoginBinding
import android.content.Context
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

        // Browse as Guest
        binding.guestButton.setOnClickListener { loginAsGuest() }
    }

    private fun doLogin() {
        val email = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            toast("Enter email and password")
            return
        }

        setLoading(true)

        // user is not guest
        GuestSession.setGuest(this, false)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                setLoading(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                toast("Wrong credentials")
            }
    }

    private fun loginAsGuest() {
        setLoading(true)

        auth.signInAnonymously()
            .addOnSuccessListener {
                GuestSession.setGuest(this, true)
                GuestSession.setFirstLaunchDone(this)
                setLoading(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                toast("Guest login failed")
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.guestButton.isEnabled = !loading
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
