package com.example.inventoryapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.data.InventoryDatabase
import com.example.inventoryapp.datawedge.DataWedgeManager

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.iv_logo)
        val progress = findViewById<ProgressBar>(R.id.progress_bar)

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 800
            fillAfter = true
        }
        logo.startAnimation(fadeIn)

        // Initialize the database on first launch so MainActivity is ready immediately
        InventoryDatabase.getInstance(this)

        // Set up DataWedge profile for this app while the splash is visible
        val dwManager = DataWedgeManager(this)
        dwManager.createInventoryProfile()

        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DELAY_MS)
    }
}