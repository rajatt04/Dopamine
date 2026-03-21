package com.google.android.piyush.dopamine.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.authentication.repository.UserAuthRepositoryImpl
import com.google.android.piyush.dopamine.authentication.viewModel.UserAuthViewModel
import com.google.android.piyush.dopamine.authentication.viewModel.UserAuthViewModelFactory
import com.google.android.piyush.dopamine.databinding.ActivityMainBinding
import com.google.android.piyush.dopamine.utilities.ToastUtilities.showToast
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userRepository : UserAuthRepositoryImpl
    private lateinit var userViewModelFactory: UserAuthViewModelFactory
    private lateinit var userViewModel: UserAuthViewModel
    private var backPressed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        userRepository = UserAuthRepositoryImpl(context = this)
        userViewModelFactory = UserAuthViewModelFactory(userRepository)
        userViewModel = ViewModelProvider(this, userViewModelFactory)[UserAuthViewModel::class.java]
        setContentView(binding.root)


        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.phoneSignIn!!.setOnClickListener{
            startActivity(Intent(this, MobileLoginActivity::class.java))
        }

        onBackPressedDispatcher.addCallback {
            if(!backPressed){
                showToast(context = applicationContext,"Press Again To Exit")
                backPressed = true
            }else{
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finishAffinity()
                finish()
                exitProcess(0)
            }
        }

        if(userViewModel.currentUser()!=null || isMobileUserLoggedIn()){
            startActivity(
                Intent(
                    this,
                    DopamineHome::class.java
                )
            )
        }

        val legacyLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){ result ->
            if(result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val signInResult = userRepository.signInWithIntent(
                        intent = result.data ?: return@launch
                    )
                    userViewModel.onSignInResult(signInResult)
                }
            }else{
                Snackbar.make(
                    binding.main, "Sign-in cancelled", Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        val oneTapLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ){ result ->
            if(result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val signInResult = userRepository.signInWithIntent(
                        intent = result.data ?: return@launch
                    )
                    userViewModel.onSignInResult(signInResult)
                }
            }else{
                lifecycleScope.launch {
                    val legacyIntent = userRepository.getLegacySignInIntent()
                    legacyLauncher.launch(legacyIntent)
                }
            }
        }

        lifecycleScope.launch {
            userViewModel.state.collect { state ->
                if(state.isSignInSuccessful){
                    applicationContext.getSharedPreferences("currentUser", MODE_PRIVATE).edit()
                        .putString("loginType", "google")
                        .putString("uid", state.userData?.userId ?: Firebase.auth.currentUser?.uid)
                        .putString("name", state.userData?.userName ?: Firebase.auth.currentUser?.displayName)
                        .putString("email", state.userData?.userEmail ?: Firebase.auth.currentUser?.email)
                        .putString("photoUrl", state.userData?.userImage ?: Firebase.auth.currentUser?.photoUrl?.toString() ?: "")
                        .apply()
                    startActivity(
                        Intent(this@MainActivity, DopamineHome::class.java)
                    )
                    userViewModel.resetSignInState()
                }

                state.signInError?.let { error ->
                    Snackbar.make(
                        binding.main, error, Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.googleSignIn.setOnClickListener{
            lifecycleScope.launch {
                val signInIntentSender = userRepository.googleSignIn()
                if (signInIntentSender != null) {
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(signInIntentSender).build()
                    )
                } else {
                    val legacyIntent = userRepository.getLegacySignInIntent()
                    legacyLauncher.launch(legacyIntent)
                }
            }
        }
    }

    private fun isMobileUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("currentUser", MODE_PRIVATE)
        return sharedPrefs.getString("loginType", "") == "mobile"
    }
}