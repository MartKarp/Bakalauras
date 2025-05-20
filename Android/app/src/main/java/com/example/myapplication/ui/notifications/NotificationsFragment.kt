package com.example.myapplication.ui.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray

class NotificationsFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()

    private val alertHandler = Handler(Looper.getMainLooper())
    private val alertPollInterval = 15000L // kas 15 sekundžių

    private var lastAlertTimestamp: String? = null

    private val pollAlerts = object : Runnable {
        override fun run() {
            val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
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
                        var showPopup = false
                        var newestTimestamp: String? = null

                        for (i in 0 until alertsArray.length()) {
                            val alert = alertsArray.getJSONObject(i)
                            val msg = alert.getString("message")
                            val time = alert.getString("timestamp")

                            if (i == 0) newestTimestamp = time
                            alerts.add("🚨 $msg\n🕒 Laikas: $time")
                        }

                        withContext(Dispatchers.Main) {
                            if (alerts.isNotEmpty()) {
                                val alertsText = alerts.joinToString("\n\n")
                                view?.findViewById<TextView>(R.id.alertTextView)?.text = alertsText

                                // Check for new alert since last poll
                                if (newestTimestamp != null && newestTimestamp != lastAlertTimestamp) {
                                    lastAlertTimestamp = newestTimestamp

                                    // 🔔 Play tone
                                    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                    tone.startTone(ToneGenerator.TONE_PROP_BEEP)

                                    // 💬 Show snackbar
                                    Snackbar.make(requireView(), "⚠️ Naujas judesio įspėjimas!", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Polling", "Nepavyko gauti įspėjimų: ${e.message}")
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
