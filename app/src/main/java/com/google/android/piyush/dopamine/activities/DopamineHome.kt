package com.google.android.piyush.dopamine.activities

import Home
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.ActivityDopamineHomeBinding
import com.google.android.piyush.dopamine.fragments.Library
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.DopamineHomeViewModel
import com.google.android.piyush.dopamine.viewModels.SharedViewModel

class DopamineHome : AppCompatActivity() {

    private val viewModel : DopamineHomeViewModel by viewModels<DopamineHomeViewModel>()
    private val sharedViewModel: SharedViewModel by viewModels() 
    private lateinit var binding: ActivityDopamineHomeBinding

    // Fragment caching
    private val homeFragment = Home()
    private val libraryFragment = Library()
    private var activeFragment: Fragment = homeFragment

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDopamineHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup MotionLayout Listener
        binding.mainMotionLayout.setTransitionListener(object : androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: androidx.constraintlayout.motion.widget.MotionLayout?, startId: Int, endId: Int) {}

            override fun onTransitionChange(motionLayout: androidx.constraintlayout.motion.widget.MotionLayout?, startId: Int, endId: Int, progress: Float) {
                // Pass progress to fragment
                val fragment = supportFragmentManager.findFragmentById(R.id.player_fragment_container)
                if (fragment is com.google.android.piyush.dopamine.fragments.YoutubePlayerFragment) {
                    fragment.setMotionProgress(progress)
                }
            }

            override fun onTransitionCompleted(motionLayout: androidx.constraintlayout.motion.widget.MotionLayout?, currentId: Int) {}

            override fun onTransitionTrigger(motionLayout: androidx.constraintlayout.motion.widget.MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
        })

        onBackPressedDispatcher.addCallback {
            if (binding.mainMotionLayout.currentState == R.id.end) {
                binding.mainMotionLayout.transitionToState(R.id.start)
            } else {
                finishAffinity()
            }
        }

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }


        if(!NetworkUtilities.isNetworkAvailable(this)){
            Utilities.turnOnNetworkDialog(this,"No Internet Connection")
        }

        if (savedInstanceState == null) {
            // Add all fragments but only show home
            supportFragmentManager.beginTransaction().apply {
                add(R.id.frameLayout, libraryFragment, "library").hide(libraryFragment)
                add(R.id.frameLayout, homeFragment, "home")
            }.commit()
        }

        setupBottomNavigation()
        setupPlayerObserver()
    }

    private fun setupPlayerObserver() {
        // Hide player initially
        hidePlayer()

        sharedViewModel.currentVideo.observe(this) { video ->
            if (video != null) {
                showPlayer()
            } else {
                hidePlayer()
            }
        }
    }

    private fun showPlayer() {
        // Restore player visibility in the MotionLayout constraint sets
        binding.mainMotionLayout.getConstraintSet(R.id.start)
            ?.setVisibility(R.id.player_fragment_container, android.view.View.VISIBLE)
        binding.mainMotionLayout.getConstraintSet(R.id.end)
            ?.setVisibility(R.id.player_fragment_container, android.view.View.VISIBLE)
        binding.playerFragmentContainer.visibility = android.view.View.VISIBLE
        // Expand the player
        if (binding.mainMotionLayout.currentState != R.id.end) {
            binding.mainMotionLayout.transitionToState(R.id.end)
        }
    }

    private fun hidePlayer() {
        // Snap MotionLayout to start state immediately (no animation)
        binding.mainMotionLayout.progress = 0f
        // Hide the player in BOTH constraint sets so MotionLayout doesn't override
        binding.mainMotionLayout.getConstraintSet(R.id.start)
            ?.setVisibility(R.id.player_fragment_container, android.view.View.GONE)
        binding.mainMotionLayout.getConstraintSet(R.id.end)
            ?.setVisibility(R.id.player_fragment_container, android.view.View.GONE)
        binding.playerFragmentContainer.visibility = android.view.View.GONE
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.library -> {
                    switchFragment(libraryFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(target)
        }.commit()
        activeFragment = target
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedFragment.value?.let {
            outState.putInt("selectedFragment", it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.setSelectedFragment(
            savedInstanceState.getInt("selectedFragment")
        )
    }
}