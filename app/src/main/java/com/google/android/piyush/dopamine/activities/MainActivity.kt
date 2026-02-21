package com.google.android.piyush.dopamine.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Skip login — go directly to home
        startActivity(Intent(this, DopamineHome::class.java))
        finish()
    }
}