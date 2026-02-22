package com.google.android.piyush.dopamine.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.piyush.dopamine.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Bottom sheet for player settings: playback speed and video quality.
 */
class PlayerSettingsSheet : BottomSheetDialogFragment() {

    private var currentSpeed: Float = 1.0f
    private var currentVideoId: String? = null
    private var onSpeedSelected: ((Float) -> Unit)? = null
    private var onQualitySelected: ((StreamOption) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val TAG = "PlayerSettingsSheet"

        fun newInstance(
            currentSpeed: Float,
            videoId: String?,
            onSpeedSelected: (Float) -> Unit,
            onQualitySelected: (StreamOption) -> Unit
        ): PlayerSettingsSheet {
            return PlayerSettingsSheet().apply {
                this.currentSpeed = currentSpeed
                this.currentVideoId = videoId
                this.onSpeedSelected = onSpeedSelected
                this.onQualitySelected = onQualitySelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_player_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpeedChips(view)
        setupQualityChips(view)
    }

    private fun setupSpeedChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.speedChipGroup)
        val speeds = mapOf(
            R.id.speed025 to 0.25f,
            R.id.speed05 to 0.5f,
            R.id.speed075 to 0.75f,
            R.id.speed10 to 1.0f,
            R.id.speed125 to 1.25f,
            R.id.speed15 to 1.5f,
            R.id.speed175 to 1.75f,
            R.id.speed20 to 2.0f
        )

        // Set the current selection
        speeds.entries.forEach { (chipId, speed) ->
            val chip = view.findViewById<Chip>(chipId)
            chip?.isChecked = speed == currentSpeed
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = view.findViewById<Chip>(checkedIds.first())
                val speed = chip?.tag?.toString()?.toFloatOrNull() ?: 1.0f
                onSpeedSelected?.invoke(speed)
            }
        }
    }

    private fun setupQualityChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.qualityChipGroup)
        val loadingIndicator = view.findViewById<LinearProgressIndicator>(R.id.qualityLoading)
        val qualityHeader = view.findViewById<View>(R.id.qualityHeader)

        val videoId = currentVideoId
        if (videoId == null) {
            qualityHeader.visibility = View.GONE
            chipGroup.visibility = View.GONE
            return
        }

        loadingIndicator.visibility = View.VISIBLE
        chipGroup.removeAllViews()

        scope.launch {
            try {
                val streams = NewPipeStreamExtractor.extractAllStreams(videoId)
                loadingIndicator.visibility = View.GONE

                if (streams.isEmpty()) {
                    qualityHeader.visibility = View.GONE
                    chipGroup.visibility = View.GONE
                    return@launch
                }

                streams.forEach { streamOption ->
                    val chip = Chip(requireContext()).apply {
                        text = "${streamOption.resolution} (${streamOption.format})"
                        isCheckable = true
                        tag = streamOption
                        setChipBackgroundColorResource(com.google.android.material.R.color.m3_chip_background_color)
                    }
                    chipGroup.addView(chip)
                }

                // Select first by default
                (chipGroup.getChildAt(0) as? Chip)?.isChecked = true

                chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (checkedIds.isNotEmpty()) {
                        val chip = view.findViewById<Chip>(checkedIds.first())
                        val option = chip?.tag as? StreamOption
                        if (option != null) {
                            onQualitySelected?.invoke(option)
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
                qualityHeader.visibility = View.GONE
                chipGroup.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
    }
}
