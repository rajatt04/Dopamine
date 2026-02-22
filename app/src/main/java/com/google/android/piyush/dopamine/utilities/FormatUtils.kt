package com.google.android.piyush.dopamine.utilities

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import java.text.DecimalFormat

/**
 * Shared formatting utilities used across the app.
 */
object FormatUtils {

    private val decimalFormatter by lazy { DecimalFormat("#0.0") }
    private val integerFormatter by lazy { DecimalFormat("#,##0") }

    /**
     * Formats a numeric count into a human-readable abbreviated string.
     * Examples: 1500 → "1.5K", 2300000 → "2.3M"
     */
    fun formatCount(count: Long): String {
        if (count < 1000) return count.toString()
        val suffix = charArrayOf(' ', 'K', 'M', 'B', 'T', 'P', 'E')
        val value = kotlin.math.floor(kotlin.math.log10(count.toDouble())).toInt()
        val base = value / 3
        return if (value >= 3 && base < suffix.size) {
            val scaledValue = count / Math.pow(10.0, (base * 3).toDouble())
            "${decimalFormatter.format(scaledValue)}${suffix[base]}"
        } else {
            integerFormatter.format(count)
        }
    }

    /**
     * Animates a view with a bounce/overshoot scale effect (e.g., for like button).
     */
    fun animateBounce(view: View) {
        view.animate()
            .scaleX(0.7f).scaleY(0.7f).setDuration(100)
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            }.start()
    }

    /**
     * Animates a like/dislike button with a burst effect:
     * scale down → pop up with overshoot + slight rotation.
     */
    fun animateLikeBurst(view: View) {
        view.animate()
            .scaleX(0.5f).scaleY(0.5f).rotation(-15f).setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1.2f).scaleY(1.2f).rotation(10f).setDuration(150)
                    .setInterpolator(OvershootInterpolator(3f))
                    .withEndAction {
                        view.animate()
                            .scaleX(1.0f).scaleY(1.0f).rotation(0f).setDuration(120)
                            .start()
                    }.start()
            }.start()
    }

    /**
     * Animates a subscribe button with a scale morph effect.
     */
    fun animateSubscribe(view: View) {
        view.animate()
            .scaleX(0.85f).scaleY(0.85f).setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.05f).scaleY(1.05f).setDuration(150)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .withEndAction {
                        view.animate()
                            .scaleX(1.0f).scaleY(1.0f).setDuration(100)
                            .start()
                    }.start()
            }.start()
    }

    /**
     * Triggers haptic feedback on a view (light click feel).
     */
    fun triggerHaptic(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.CONFIRM,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Formats milliseconds into a human-readable time string (e.g., "1:23:45" or "3:45").
     */
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

