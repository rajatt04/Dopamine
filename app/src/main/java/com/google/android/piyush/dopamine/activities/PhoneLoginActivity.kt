package com.google.android.piyush.dopamine.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.piyush.dopamine.databinding.ActivityPhoneLoginBinding
import com.google.android.piyush.dopamine.utilities.ToastUtilities.showToast

class PhoneLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnGetOtp.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.length == 10) {
                // Determine country code - defaulting to +91 for now as per design
                val fullPhoneNumber = "+91$phoneNumber"
                
                val intent = Intent(this, OtpVerifyActivity::class.java)
                intent.putExtra("phoneNumber", fullPhoneNumber)
                startActivity(intent)
            } else {
                showToast(this, "Please enter a valid 10-digit number")
            }
        }
    }
}
