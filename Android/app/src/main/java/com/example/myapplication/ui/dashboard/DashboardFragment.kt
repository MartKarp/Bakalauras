package com.example.myapplication.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class DashboardFragment : Fragment() {

    private val client = OkHttpClient()
    private val serverUrl = "http://194.31.55.182:3001"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val lockButton = view.findViewById<Button>(R.id.lockButton)
        val unlockButton = view.findViewById<Button>(R.id.unlockButton)

        lockButton.setOnClickListener {
            sendCommandToServer("/api/lock")
        }

        unlockButton.setOnClickListener {
            sendCommandToServer("/api/unlock")
        }

        return view
    }

    private fun sendCommandToServer(endpoint: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(serverUrl + endpoint)
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Command sent: ${endpoint.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to send command", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
