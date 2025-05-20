package com.example.myapplication.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.services.AlertPollingService
import com.example.myapplication.ui.register.RegisterActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {

    private val serverUrl = "http://194.31.55.182:3001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val goToRegister = findViewById<android.widget.TextView>(R.id.goToRegister)

        loginButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            // 🔐 Validate inputs
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Įveskite teisingą el. paštą", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Toast.makeText(this, "Slaptažodis turi būti bent 6 simbolių", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 📦 Create JSON request
            val json = JSONObject().apply {
                put("email", emailText)
                put("password", passwordText)
            }

            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverUrl/api/login")
                .post(body)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                Log.d("LOGIN", "Server response: $responseBody")

                if (response.isSuccessful) {
                    try {
                        val json = JSONObject(responseBody)

                        if (!json.has("token")) {
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Server returned no token", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val token = json.getString("token")

                        // Save JWT token
                        getSharedPreferences("auth", MODE_PRIVATE).edit().putString("jwt", token).apply()

                        // 🔍 Check if the user has claimed a device
                        val meRequest = Request.Builder()
                            .url("$serverUrl/api/me")
                            .addHeader("Authorization", token)
                            .build()

                        val meResponse = client.newCall(meRequest).execute()
                        val meJson = JSONObject(meResponse.body?.string() ?: "")
                        val devices = meJson.getJSONArray("devices")

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)

                        if (devices.length() == 0) {
                            intent.putExtra("goToClaim", true)
                        }

                        val serviceIntent = Intent(this@LoginActivity, AlertPollingService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }



                        startActivity(intent)
                        finish()

                    } catch (e: Exception) {
                        Log.e("LOGIN", "Failed to parse response: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Invalid response format", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login failed: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

        // 🔁 Go to register activity
        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
