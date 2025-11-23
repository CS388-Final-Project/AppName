package com.example.cs388finalproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.cs388finalproject.databinding.ActivityMainBinding
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.ui.auth.SignupActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val firstLaunch = prefs.getBoolean("first_launch", true)

        if(firstLaunch) {
            startActivity(Intent(this, SignupActivity::class.java))
            prefs.edit().putBoolean("first_launch", false).apply()
            finish()
            return
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }
}
