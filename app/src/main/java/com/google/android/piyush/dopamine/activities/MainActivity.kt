package com.google.android.piyush.dopamine.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.ActivityMainBinding
import com.google.android.piyush.dopamine.utilities.PreferenceManager
import com.google.android.piyush.dopamine.utilities.ToastUtilities.showToast
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var backPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (PreferenceManager.isLoggedIn(this)) {
            navigateToDopamineHome()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.phoneSignIn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Under Development")
                .setMessage("OTP Verification Will Be Available Soon")
                .setPositiveButton("Ok", null)
                .show()
        }

        binding.googleSignIn.setOnClickListener {
            // After successful Google Sign In, save login state
            // This is a placeholder - implement your actual Google Sign In logic
            handleSuccessfulLogin("user_id_123", "user@example.com", "User Name")
        }

        onBackPressedDispatcher.addCallback {
            if (!backPressed) {
                showToast(context = applicationContext, "Press Again To Exit")
                backPressed = true
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finishAffinity()
                finish()
                exitProcess(0)
            }
        }
    }

    private fun handleSuccessfulLogin(userId: String, email: String, name: String) {
        // Save user data and login state
        PreferenceManager.saveUserData(this, userId, email, name)
        PreferenceManager.setLoggedIn(this, true)

        // Navigate to DopamineHome
        navigateToDopamineHome()
    }

    private fun navigateToDopamineHome() {
        val intent = Intent(this, DopamineHome::class.java)
        startActivity(intent)
        finish() // Close MainActivity so user can't go back to login screen
    }
}