package com.google.android.piyush.dopamine.utilities


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.piyush.database.model.CustomPlaylistView
import com.google.android.piyush.database.viewModel.DatabaseViewModel
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.databinding.ItemCustomDialogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NetworkUtilities {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    fun showNetworkError(context: Context?) {
        context ?: return
        MaterialAlertDialogBuilder(context)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle("Error!")
            .setCancelable(false)
            .setMessage("No Internet Connection.")
            .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }
}

object ToastUtilities {
    fun showToast(context: Context?, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

object Utilities {
const val PERMISSION_REQUEST_CODE = 100
    const val RELEASE_DATE = "07/04/2024"
    const val PROJECT_VERSION = "dopamine_20240704_01.phone.stable.dynamic"
    const val PRE_RELEASE_VERSION = "dopamine_20242003_01.phone.prerelease.dynamic"
    const val STABLE = "stable"
    const val DEFAULT_LOGO = "https://cdn-images-1.medium.com/v2/resize:fit:1200/1*3tLD4Ve66pbBpuawm9Fu9Q.png"
    const val DEFAULT_BANNER = "https://developer.android.com/static/images/social/android-developers.png"
    const val LIGHT_MODE = "light"
    const val DARK_MODE = "dark"
    const val SYSTEM_MODE = "system"
    val THEME = arrayOf(
        LIGHT_MODE,
        DARK_MODE,
        SYSTEM_MODE
    )
    const val GITHUB = "https://github.com/kotlindevs/dopamine"
    const val EMAIL = "kotlindevslife@gmail.com"
    const val EMAIL1 = "piyushmakwana5617@gmail.com"


    fun turnOnNetworkDialog(context: Context, message: String) = MaterialAlertDialogBuilder(context).also {
        it.setTitle("Network not detected")
        it.setMessage("Please turn on network to view the $message.")
        it.setIcon(R.drawable.wifi_off)
        it.setCancelable(true)
        it.setNegativeButton("Cancel") {
            dialog, _ ->
            dialog.dismiss()
        }
        it.setPositiveButton("Turn on") { dialog, _ ->
            dialog.dismiss()
            Intent(Settings.ACTION_WIRELESS_SETTINGS).also { intent ->
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }.create().show()
}

class CustomDialog(context: Context, private val databaseViewModel: DatabaseViewModel) : MaterialAlertDialogBuilder(context) {
    private var binding: ItemCustomDialogBinding
    init {
        setCancelable(true)
        binding = ItemCustomDialogBinding.inflate(LayoutInflater.from(context)).also {
            setView(it.root)
        }

        binding.button.setOnClickListener {
            val playlistName = binding.text1.text.toString().trim()
            val playlistDescription = binding.text2.text.toString().trim()

            CoroutineScope(Dispatchers.Main).launch {
                if(databaseViewModel.isPlaylistExist(playlistName)){
                    binding.textInputLayout1.isErrorEnabled = true
                    binding.textInputLayout1.error = "Playlist Already Exists"
                }else{
                    if(playlistName.isEmpty()){
                        ToastUtilities.showToast(context, "Please Fill All Fields")
                    }else {
                        databaseViewModel.createCustomPlaylist(
                            CustomPlaylistView(
                                playlistName,
                                playlistDescription.ifEmpty { "Empty Description" },
                            )
                        )
                        binding.text1.text?.clear()
                        binding.text2.text?.clear()
                        ToastUtilities.showToast(context, "$playlistName Created ✅")
                    }
                }
            }
        }
    }
}
