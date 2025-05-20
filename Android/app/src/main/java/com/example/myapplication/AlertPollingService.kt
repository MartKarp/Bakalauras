package com.example.myapplication.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class AlertPollingService : Service() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()
    private var pollingJob: Job? = null
    private val interval = 15000L

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        startPolling()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "alert_channel"
        val channelName = "Motion Alerts"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Judėjimo aptikimo paslauga")
            .setContentText("Ieškoma įspėjimų...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)
    }

    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val jwt = getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .getString("jwt", null)

                    if (jwt != null) {
                        val request = Request.Builder()
                            .url("$serverUrl/api/alerts")
                            .addHeader("Authorization", jwt)
                            .build()

                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: ""

                        if (response.isSuccessful) {
                            val alerts = JSONArray(body)
                            if (alerts.length() > 0) {
                                val latest = alerts.getJSONObject(0)
                                val msg = latest.getString("message")
                                showPopupNotification(msg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PollingService", "Polling error: ${e.message}")
                }

                delay(interval)
            }
        }
    }

    private fun showPopupNotification(message: String) {
        val channelId = "alert_popup"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Judėjimo Įspėjimai",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                description = "Pranešimai apie judesį"
            }

            manager.createNotificationChannel(channel) // Overwrites previous config
        }


        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ Naujas įspėjimas")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL) // Sound, vibration, lights
            .setAutoCancel(true)
            .build()


        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        Log.d("AlertService", "Showing notification: $message")

    }


    override fun onDestroy() {
        pollingJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
