package com.google.android.piyush.dopamine.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.piyush.dopamine.databinding.ActivityOtpVerifyBinding
import com.google.android.piyush.dopamine.utilities.ToastUtilities.showToast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import android.os.CountDownTimer

class OtpVerifyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerifyBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        phoneNumber = intent.getStringExtra("phoneNumber")

        if (phoneNumber != null) {
            binding.tvOtpSubtitle.text = "Enter the code we just sent to $phoneNumber"
            startPhoneNumberVerification(phoneNumber!!)
        } else {
            showToast(this, "Error: missing phone number")
            finish()
        }

        binding.btnVerify.setOnClickListener {
            val code = binding.etOtp.text.toString()
            if (code.length == 6 && verificationId != null) {
                verifyPhoneNumberWithCode(verificationId!!, code)
            } else {
                showToast(this, "Please enter a valid 6-digit code")
            }
        }

        binding.tvResendBtn.setOnClickListener {
            if (phoneNumber != null && resendToken != null) {
                resendVerificationCode(phoneNumber!!, resendToken!!)
            }
        }
        
        startSmsRetriever()
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerify.isEnabled = false
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        
        startTimer()
    }

    private fun resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken) {
        binding.progressBar.visibility = View.VISIBLE
         val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        startTimer()
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            
            val code = credential.smsCode
            if (code != null) {
                binding.etOtp.setText(code)
                verifyPhoneNumberWithCode(verificationId ?: "", code)
            } else {
                 signInWithPhoneAuthCredential(credential)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            binding.progressBar.visibility = View.GONE
            binding.btnVerify.isEnabled = true
            showToast(this@OtpVerifyActivity, "Verification Failed: ${e.message}")
            Log.e("OtpVerify", "Verification Failed", e)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            binding.progressBar.visibility = View.GONE
            binding.btnVerify.isEnabled = true
            
            this@OtpVerifyActivity.verificationId = verificationId
            this@OtpVerifyActivity.resendToken = token
            showToast(this@OtpVerifyActivity, "Code Sent")
        }
    }

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerify.isEnabled = false
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    showToast(this, "Login Successful!")
                    // Navigate to Home or MainActivity
                    val intent = Intent(this, DopamineHome::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                    showToast(this, "Login Failed: ${task.exception?.message}")
                }
            }
    }
    
    private fun startTimer() {
        binding.tvResendTimer.visibility = View.VISIBLE
        binding.tvResendBtn.visibility = View.GONE
        
        object : CountDownTimer(60000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                binding.tvResendTimer.text = "Resend Code in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.tvResendTimer.visibility = View.GONE
                binding.tvResendBtn.visibility = View.VISIBLE
            }
        }.start()
    }
    
    private fun startSmsRetriever() {
        val client = SmsRetriever.getClient(this)
        val task = client.startSmsRetriever()
        task.addOnSuccessListener {
            // Successfully started listener
        }
        task.addOnFailureListener {
            // Failed to start listener
        }
    }
}
