package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val jwt = getSharedPreferences("auth", MODE_PRIVATE).getString("jwt", null)

        if (jwt.isNullOrEmpty()) {
            // No token found, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 🔁 Check if user has any devices
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
                    // 🚫 Hide nav and force claim screen
                    navView.visibility = View.GONE
                    navController.navigate(R.id.navigation_claim)
                } else {
                    // ✅ User has device, continue
                    navView.visibility = View.VISIBLE
                }
            }
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_claim // 👈 we'll define this next
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }


}