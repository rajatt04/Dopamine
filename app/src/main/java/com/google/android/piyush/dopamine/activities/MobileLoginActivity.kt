package com.google.android.piyush.dopamine.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.ActivityMobileLoginBinding

class MobileLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMobileLoginBinding
    private var countDownTimer: CountDownTimer? = null
    private var selectedCountryCode = "+91"

    private val countries = listOf(
        "+91 India",
        "+1 United States",
        "+44 United Kingdom",
        "+61 Australia",
        "+81 Japan",
        "+86 China",
        "+49 Germany",
        "+33 France",
        "+55 Brazil",
        "+7 Russia"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMobileLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCountryDropdown()
        setupPhoneInput()
        setupButtons()
        setupOtpInput()
        animateEntrance()
    }

    private fun setupCountryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countries)
        (binding.countryCodeDropdown as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            setText(countries[0], false)
            setOnItemClickListener { _, _, position, _ ->
                selectedCountryCode = countries[position].split(" ")[0]
            }
        }
    }

    private fun setupPhoneInput() {
        binding.phoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val isValid = (s?.length ?: 0) >= 10
                binding.btnSendOtp.isEnabled = isValid
                binding.btnSendOtp.alpha = if (isValid) 1.0f else 0.5f
                binding.phoneInputLayout.error = null
            }
        })
    }

    private fun setupOtpInput() {
        binding.otpInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val isValid = (s?.length ?: 0) == 6
                binding.btnVerifyOtp.isEnabled = isValid
                binding.btnVerifyOtp.alpha = if (isValid) 1.0f else 0.5f
            }
        })
    }

    private fun setupButtons() {
        binding.btnSendOtp.alpha = 0.5f
        binding.btnVerifyOtp.alpha = 0.5f
        binding.btnVerifyOtp.isEnabled = false
        binding.btnSendOtp.isEnabled = false

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.phoneInput.text.toString().trim()
            if (phone.length < 10) {
                binding.phoneInputLayout.error = "Please enter a valid phone number"
                shakeView(binding.phoneInputLayout)
                return@setOnClickListener
            }
            simulateSendOtp(phone)
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.otpInput.text.toString()
            if (otp == "123456") {
                showLoadingState()
            } else {
                binding.otpInput.error = "Invalid code"
                shakeView(binding.otpInput)
                Snackbar.make(binding.root, "Invalid verification code", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.btnVerifyOtp)
                    .show()
            }
        }

        binding.btnBackToPhone.setOnClickListener {
            showPhoneStep()
        }

        binding.btnResendOtp.setOnClickListener {
            startResendTimer()
            Snackbar.make(binding.root, "Code resent successfully", Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.btnResendOtp)
                .show()
        }

        binding.btnGoogleLogin.setOnClickListener {
            Snackbar.make(binding.root, "Opening Google Sign-In...", Snackbar.LENGTH_SHORT)
                .setAnchorView(it)
                .show()
            finish()
        }

        binding.btnFacebookLogin.setOnClickListener {
            Snackbar.make(binding.root, "Facebook login coming soon", Snackbar.LENGTH_SHORT)
                .setAnchorView(it)
                .show()
        }
    }

    private fun simulateSendOtp(phone: String) {
        binding.btnSendOtp.text = ""
        binding.btnSendOtp.icon = null

        val loadingButton = binding.btnSendOtp
        loadingButton.isEnabled = false

        binding.root.postDelayed({
            binding.tvOtpSent.text = "Code sent to $selectedCountryCode $phone"
            showOtpStep()
        }, 1500)
    }

    private fun showOtpStep() {
        binding.btnSendOtp.text = "Continue"
        binding.btnSendOtp.icon = resources.getDrawable(R.drawable.ic_arrow_forward, theme)
        binding.btnSendOtp.isEnabled = true

        animateTransition(binding.phoneLayout, binding.otpLayout)
        binding.stepProgress.progress = 100
        binding.otpInput.setText("")
        binding.otpInput.requestFocus()
        startResendTimer()
    }

    private fun showPhoneStep() {
        countDownTimer?.cancel()
        animateTransition(binding.otpLayout, binding.phoneLayout)
        binding.stepProgress.progress = 50
    }

    private fun animateTransition(hideView: View, showView: View) {
        val hideAnimator = ObjectAnimator.ofFloat(hideView, "alpha", 1f, 0f).apply {
            duration = 200
        }
        val hideTranslation = ObjectAnimator.ofFloat(hideView, "translationX", 0f, -50f).apply {
            duration = 200
        }

        hideAnimator.addUpdateListener {
            if (it.animatedFraction >= 0.8f) {
                hideView.visibility = View.GONE
                showView.visibility = View.VISIBLE
                showView.alpha = 0f
                showView.translationX = 50f

                val showAnimator = ObjectAnimator.ofFloat(showView, "alpha", 0f, 1f).apply {
                    duration = 250
                    interpolator = AccelerateDecelerateInterpolator()
                }
                val showTranslation = ObjectAnimator.ofFloat(showView, "translationX", 50f, 0f).apply {
                    duration = 250
                    interpolator = OvershootInterpolator(0.8f)
                }
                AnimatorSet().apply {
                    playTogether(showAnimator, showTranslation)
                    start()
                }
            }
        }

        AnimatorSet().apply {
            playTogether(hideAnimator, hideTranslation)
            start()
        }
    }

    private fun showLoadingState() {
        binding.otpLayout.visibility = View.GONE
        binding.dividerSection.visibility = View.GONE
        binding.socialLoginRow.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE

        val fadeIn = ObjectAnimator.ofFloat(binding.loadingLayout, "alpha", 0f, 1f).apply {
            duration = 300
        }
        val scaleUpX = ObjectAnimator.ofFloat(binding.loadingLayout, "scaleX", 0.8f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(1.2f)
        }
        val scaleUpY = ObjectAnimator.ofFloat(binding.loadingLayout, "scaleY", 0.8f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(1.2f)
        }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleUpX, scaleUpY)
            start()
        }

        binding.root.postDelayed({
            loginUser()
        }, 2500)
    }

    private fun loginUser() {
        val phoneNumber = binding.phoneInput.text.toString().trim()
        val fullPhone = "$selectedCountryCode $phoneNumber"
        val uid = "mobile_$phoneNumber"

        val sharedPrefs = getSharedPreferences("currentUser", MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("uid", uid)
            .putString("name", "Mobile User")
            .putString("email", fullPhone)
            .putString("phone", fullPhone)
            .putString("photoUrl", "https://ui-avatars.com/api/?name=MU&background=C00100&color=fff&size=128")
            .putString("loginType", "mobile")
            .apply()

        binding.tvLoadingMessage.text = "Welcome back!"

        binding.root.postDelayed({
            val intent = Intent(this, DopamineHome::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 800)
    }

    private fun startResendTimer() {
        binding.btnResendOtp.isEnabled = false
        binding.btnResendOtp.alpha = 0.5f
        binding.tvResendTimer.visibility = View.VISIBLE

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvResendTimer.text = " (${seconds}s)"
            }

            override fun onFinish() {
                binding.btnResendOtp.isEnabled = true
                binding.btnResendOtp.alpha = 1.0f
                binding.tvResendTimer.visibility = View.GONE
            }
        }.start()
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.lottieHeader,
            binding.tvWelcomeBack,
            binding.tvSubtitle,
            binding.contentCard
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 60f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay((index * 120).toLong())
                .setInterpolator(OvershootInterpolator(0.7f))
                .start()
        }

        binding.contentCard.alpha = 0f
        binding.contentCard.translationY = 100f
        binding.contentCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 20f, -20f, 10f, -10f, 5f, -5f, 0f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }
        shake.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
