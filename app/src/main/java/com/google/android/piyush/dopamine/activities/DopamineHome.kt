package com.google.android.piyush.dopamine.activities

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
import com.google.android.piyush.dopamine.fragments.Home
import com.google.android.piyush.dopamine.fragments.Library
import com.google.android.piyush.dopamine.fragments.Search
import com.google.android.piyush.dopamine.fragments.Shorts
import com.google.android.piyush.dopamine.utilities.NetworkUtilities
import com.google.android.piyush.dopamine.utilities.Utilities
import com.google.android.piyush.dopamine.viewModels.DopamineHomeViewModel
import com.google.android.piyush.dopamine.viewModels.SharedViewModel
import kotlin.system.exitProcess

@Suppress("DEPRECATION")
class DopamineHome : AppCompatActivity() {

    private val viewModel : DopamineHomeViewModel by viewModels<DopamineHomeViewModel>()
    private val sharedViewModel: SharedViewModel by viewModels() 
    private lateinit var binding: ActivityDopamineHomeBinding

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
                 overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out
                )
                finishAffinity()
                finish()
                exitProcess(0)
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
            defaultScreen(Home())
        }

        setupBottomNavigation()
        setupPlayerObserver()
    }

    private fun setupPlayerObserver() {
        // Hide player initially
        binding.playerFragmentContainer.visibility = android.view.View.GONE

        sharedViewModel.currentVideo.observe(this) { video ->
            if (video != null) {
                binding.playerFragmentContainer.visibility = android.view.View.VISIBLE
                // Expand the player when a new video is selected
                 // Check if it's already visible to avoid re-animation if needed, 
                 // but typically selecting a video should open it.
                 // We can check if video ID changed or force expand.
                 // For now, force expand.
                 if (binding.mainMotionLayout.currentState != R.id.end) {
                     binding.mainMotionLayout.transitionToState(R.id.end)
                 }
            } else {
                binding.playerFragmentContainer.visibility = android.view.View.GONE
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    defaultScreen(Home())
                    true
                }
                R.id.search -> {
                    defaultScreen(Search())
                    true
                }
                R.id.library -> {
                    defaultScreen(Library())
                    true
                }
                R.id.shorts -> {
                    defaultScreen(Shorts())
                    true
                }
                else -> false
            }
        }
    }

    private fun defaultScreen(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout,fragment)
        fragmentTransaction.commit()
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