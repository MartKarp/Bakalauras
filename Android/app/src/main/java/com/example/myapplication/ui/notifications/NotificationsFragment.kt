package com.example.myapplication.ui.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class NotificationsFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()

    private val alertHandler = Handler(Looper.getMainLooper())
    private val alertPollInterval = 15000L // 15 sekundžių

    private var lastAlertTimestamp: String? = null
    private val alerts = mutableListOf<Pair<String, String>>() // (id, formattedText)

    private lateinit var alertTextView: TextView
    private lateinit var deleteAllButton: Button

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
                        val newAlerts = mutableListOf<Pair<String, String>>()
                        var newestTimestamp: String? = null

                        for (i in 0 until alertsArray.length()) {
                            val alert = alertsArray.getJSONObject(i)
                            val id = alert.getString("_id")
                            val msg = alert.getString("message")
                            val time = alert.getString("timestamp")

                            if (i == 0) newestTimestamp = time
                            val text = "🚨 $msg\n🕒 Laikas: $time"
                            newAlerts.add(Pair(id, text))
                        }

                        withContext(Dispatchers.Main) {
                            alerts.clear()
                            alerts.addAll(newAlerts)

                            updateAlertText()

                            // New alert sound & notification
                            if (newestTimestamp != null && newestTimestamp != lastAlertTimestamp) {
                                lastAlertTimestamp = newestTimestamp

                                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                tone.startTone(ToneGenerator.TONE_PROP_BEEP)

                                Snackbar.make(requireView(), "⚠️ Naujas judesio įspėjimas!", Snackbar.LENGTH_LONG).show()
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

    private fun updateAlertText() {
        if (alerts.isEmpty()) {
            alertTextView.text = "Nėra aktyvių įspėjimų"
            return
        }

        val builder = StringBuilder()
        for ((i, alert) in alerts.withIndex()) {
            builder.append("🔔 ${alert.second}\n[Palieskite, kad pašalintumėte]\n\n")
        }

        alertTextView.text = builder.toString()
    }

    private fun deleteAlertByIndex(index: Int) {
        val (alertId, _) = alerts[index]
        val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("$serverUrl/api/alerts/$alertId")
                .delete()
                .addHeader("Authorization", jwt)
                .build()

            client.newCall(request).execute().close()

            withContext(Dispatchers.Main) {
                alerts.removeAt(index)
                updateAlertText()
                Snackbar.make(requireView(), "✅ Įspėjimas pašalintas", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAllAlerts() {
        val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            for ((id, _) in alerts) {
                val request = Request.Builder()
                    .url("$serverUrl/api/alerts/$id")
                    .delete()
                    .addHeader("Authorization", jwt)
                    .build()

                client.newCall(request).execute().close()
            }

            withContext(Dispatchers.Main) {
                alerts.clear()
                updateAlertText()
                Snackbar.make(requireView(), "🗑️ Visi įspėjimai ištrinti", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        alertTextView = view.findViewById(R.id.alertTextView)
        deleteAllButton = view.findViewById(R.id.deleteAllButton)

        // 📌 Touch to delete individual alert
        alertTextView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val layout = alertTextView.layout
                val line = layout.getLineForVertical(event.y.toInt())
                val index = line / 3 // each alert takes 3 lines

                if (index in alerts.indices) {
                    deleteAlertByIndex(index)
                    return@setOnTouchListener true
                }
            }
            false
        }

        deleteAllButton.setOnClickListener {
            deleteAllAlerts()
        }

        return view
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
