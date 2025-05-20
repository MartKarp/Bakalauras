package com.example.myapplication.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class DashboardFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()
    private lateinit var lockStatusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        lockStatusText = view.findViewById(R.id.lockStatusText)
        val lockButton = view.findViewById<Button>(R.id.lockButton)
        val unlockButton = view.findViewById<Button>(R.id.unlockButton)

        checkLockStatus() // 🔍 Check current status on load

        lockButton.setOnClickListener {
            sendLockCommand("/api/lock", "Užrakinta")
        }

        unlockButton.setOnClickListener {
            sendLockCommand("/api/unlock", "Atrakinta")
        }

        return view
    }

    private fun checkLockStatus() {
        val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt", null) ?: return

        val request = Request.Builder()
            .url("$serverUrl/api/lock")
            .addHeader("Authorization", jwt)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val status = json.getString("status")

                    val statusText = if (status == "locked") "Užrakinta" else "Atrakinta"

                    CoroutineScope(Dispatchers.Main).launch {
                        lockStatusText.text = statusText
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendLockCommand(endpoint: String, statusText: String) {
        val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt", null) ?: return

        val request = Request.Builder()
            .url("$serverUrl$endpoint")
            .post("".toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", jwt)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        lockStatusText.text = statusText
                        Toast.makeText(context, "Užrakto būsena atnaujinta", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⚠Nepavyko atnaujinti", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

