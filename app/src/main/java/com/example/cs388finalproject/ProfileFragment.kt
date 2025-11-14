package com.example.cs388finalproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cs388finalproject.databinding.FragmentProfileBinding
import com.example.cs388finalproject.ui.auth.LoginActivity
import com.example.cs388finalproject.ui.home.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.Continuation

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserData()

        // Logout button
        binding.btnLogout.setOnClickListener {
            auth.signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {

        val user = auth.currentUser
        if (user == null) {
            return
        }

        binding.tvEmail.text = user.email ?: "Email Not Available"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                binding.tvUsername.text = username ?: "N/A"
            }
            .addOnFailureListener {
                binding.tvUsername.text = "Error Loading Username"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
