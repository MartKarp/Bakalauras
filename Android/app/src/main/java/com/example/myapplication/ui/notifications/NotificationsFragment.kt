package com.example.myapplication.ui.notifications

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class NotificationsFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()

    private val alertHandler = Handler(Looper.getMainLooper())
    private val alertPollInterval = 15000L // 15 seconds

    private val pollAlerts = object : Runnable {
        override fun run() {
            val jwt = requireContext()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("jwt", null) ?: return

            val request = Request.Builder()
                .url("$serverUrl/api/alerts")
                .addHeader("Authorization", jwt)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val alertsArray = JSONArray(responseBody)

                        val alerts = mutableListOf<String>()
                        for (i in 0 until alertsArray.length()) {
                            val alert = alertsArray.getJSONObject(i)
                            val msg = alert.getString("message")
                            val time = alert.getString("timestamp")
                            alerts.add("🔔 $msg\n🕒 $time")
                        }

                        withContext(Dispatchers.Main) {
                            val alertsText = alerts.joinToString("\n\n")
                            view?.findViewById<TextView>(R.id.alertTextView)?.text = alertsText
                        }
                    } else {
                        Log.e("Polling", "Response failed: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("Polling", "Failed to poll alerts: ${e.message}")
                }
            }

            alertHandler.postDelayed(this, alertPollInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onResume() {
        super.onResume()
        alertHandler.post(pollAlerts)
    }

    override fun onPause() {
        super.onPause()
        alertHandler.removeCallbacks(pollAlerts)
    }
}
