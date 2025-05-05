package com.example.myapplication.ui.dashboard

import android.os.Bundle
import android.util.Log
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
                val url = serverUrl + endpoint
                Log.d("BikeLock", "Sending POST to $url")

                val request = Request.Builder()
                    .url(url)
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        Log.d("BikeLock", "Server response: ${response.code}")
                        Toast.makeText(context, "Command sent: ${endpoint.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("BikeLock", "Failed with code: ${response.code}")
                        Toast.makeText(context, "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BikeLock", "Request failed: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}
