package com.google.android.piyush.dopamine.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.android.piyush.dopamine.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val splashContent = findViewById<View>(R.id.splashContent)
        val appIcon = findViewById<View>(R.id.appIcon)
        val appName = findViewById<View>(R.id.appName)
        val appTagline = findViewById<View>(R.id.appTagline)
        val getStartedButton = findViewById<View>(R.id.getStartedButton)

        // Animate splash content
        val contentFadeIn = ObjectAnimator.ofFloat(splashContent, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 200
        }

        // Icon scale animation with overshoot
        val iconScaleX = ObjectAnimator.ofFloat(appIcon, "scaleX", 0.5f, 1f).apply {
            duration = 600
            interpolator = OvershootInterpolator(1.5f)
        }
        val iconScaleY = ObjectAnimator.ofFloat(appIcon, "scaleY", 0.5f, 1f).apply {
            duration = 600
            interpolator = OvershootInterpolator(1.5f)
        }

        // Title slide up and fade in
        val titleFadeIn = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 300
        }
        val titleSlideUp = ObjectAnimator.ofFloat(appName, "translationY", 30f, 0f).apply {
            duration = 500
            startDelay = 300
            interpolator = DecelerateInterpolator()
        }

        // Tagline slide up and fade in
        val taglineFadeIn = ObjectAnimator.ofFloat(appTagline, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 450
        }
        val taglineSlideUp = ObjectAnimator.ofFloat(appTagline, "translationY", 30f, 0f).apply {
            duration = 500
            startDelay = 450
            interpolator = DecelerateInterpolator()
        }

        // Button slide up and fade in
        val buttonFadeIn = ObjectAnimator.ofFloat(getStartedButton, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 600
        }
        val buttonSlideUp = ObjectAnimator.ofFloat(getStartedButton, "translationY", 60f, 0f).apply {
            duration = 500
            startDelay = 600
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(
                contentFadeIn,
                iconScaleX, iconScaleY,
                titleFadeIn, titleSlideUp,
                taglineFadeIn, taglineSlideUp,
                buttonFadeIn, buttonSlideUp
            )
            start()
        }

        getStartedButton.setOnClickListener {
            startActivity(Intent(this, DopamineHome::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}