package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.services.AlertPollingService
import com.example.myapplication.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val jwt = getSharedPreferences("auth", MODE_PRIVATE).getString("jwt", null)

        if (jwt.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }


        // 🔁 Check if user has a device
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://194.31.55.182:3001/api/me")
                .addHeader("Authorization", jwt)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val json = JSONObject(responseBody ?: "")
            val devices = json.getJSONArray("devices")

            runOnUiThread {
                if (devices.length() == 0) {
                    navView.visibility = View.GONE
                    navController.navigate(R.id.navigation_claim)
                } else {
                    navView.visibility = View.VISIBLE
                }
            }
        }
        val intent = Intent(this, AlertPollingService::class.java)
        startService(intent)



        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_claim,
                R.id.navigation_profile
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "motion_channel",
                "Judėjimo pranešimai",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Įspėjimai apie netikėtą judėjimą"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAlertBadge() {
        val badge = navView.getOrCreateBadge(R.id.navigation_notifications)
        badge.isVisible = true
        badge.number += 1
    }

    fun clearAlertBadge() {
        navView.removeBadge(R.id.navigation_notifications)
    }
}
