package com.google.android.piyush.dopamine.activities


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.ActivitySettingsBinding
import java.io.File

import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val databaseViewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.topAppBar.setNavigationOnClickListener { finish() }

        // Autoplay switch
        val prefs = getSharedPreferences("dopamine_settings", MODE_PRIVATE)
        binding.autoplaySwitch.isChecked = prefs.getBoolean("autoplay", true)
        binding.autoplaySwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autoplay", isChecked).apply()
        }

        // Data saver switch
        binding.dataSaverSwitch.isChecked = prefs.getBoolean("data_saver", false)
        binding.dataSaverSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("data_saver", isChecked).apply()
        }

        // Default quality
        binding.defaultQualityCard.setOnClickListener {
            val qualities = arrayOf("Auto (Recommended)", "1080p", "720p", "480p", "360p")
            val currentIndex = qualities.indexOf(binding.currentQuality.text.toString()).coerceAtLeast(0)
            MaterialAlertDialogBuilder(this)
                .setTitle("Default Video Quality")
                .setSingleChoiceItems(qualities, currentIndex) { dialog, which ->
                    binding.currentQuality.text = qualities[which]
                    prefs.edit().putString("default_quality", qualities[which]).apply()
                    dialog.dismiss()
                }
                .show()
        }
        binding.currentQuality.text = prefs.getString("default_quality", "Auto (Recommended)")

        // Clear cache
        updateCacheSize()
        binding.clearCacheCard.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Cache")
                .setMessage("This will clear all cached data. Are you sure?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                    updateCacheSize()
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Clear watch history
        binding.clearHistoryCard.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Watch History")
                .setMessage("This will delete all your recently watched videos. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    databaseViewModel.deleteRecentVideo()
                    Toast.makeText(this, "Watch history cleared", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun updateCacheSize() {
        val cacheSize = getDirSize(cacheDir)
        binding.cacheSize.text = formatSize(cacheSize)
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
