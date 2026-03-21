package com.google.android.piyush.dopamine.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.adapters.DownloadsAdapter
import com.google.android.piyush.dopamine.databinding.ActivityDownloadsBinding
import com.google.android.piyush.dopamine.viewModels.DownloadViewModel

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var downloadsAdapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeDownloads()
        setupClearButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        downloadsAdapter = DownloadsAdapter(
            context = this,
            onCancel = { download ->
                downloadViewModel.cancelDownload(download.videoId)
                Snackbar.make(binding.root, "Download cancelled", Snackbar.LENGTH_SHORT).show()
            },
            onDelete = { download ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Download")
                    .setMessage("Are you sure you want to delete this download?")
                    .setPositiveButton("Delete") { _, _ ->
                        downloadViewModel.deleteDownload(download.videoId)
                        Snackbar.make(binding.root, "Download deleted", Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onRetry = { download ->
                Snackbar.make(binding.root, "Retry not available - video URL expired", Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.downloadsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DownloadsActivity)
            adapter = downloadsAdapter
        }
    }

    private fun observeDownloads() {
        downloadViewModel.allDownloads.observe(this) { downloads ->
            downloadsAdapter.submitList(downloads)

            if (downloads.isNullOrEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.downloadsRecyclerView.visibility = View.GONE
                binding.clearCompletedButton.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.downloadsRecyclerView.visibility = View.VISIBLE
                binding.clearCompletedButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClearButton() {
        binding.clearCompletedButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Completed")
                .setMessage("Remove all completed downloads from the list?")
                .setPositiveButton("Clear") { _, _ ->
                    downloadViewModel.clearCompletedDownloads()
                    Snackbar.make(binding.root, "Completed downloads cleared", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
