package com.example.myapplication.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ProfileFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val emailText = view.findViewById<TextView>(R.id.emailInfo)
        val deviceText = view.findViewById<TextView>(R.id.deviceInfo)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        val sharedPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("jwt", null)

        // 🧠 Default placeholders
        emailText.text = "📧 El. paštas: ..."
        deviceText.text = "🔗 Įrenginys: ..."

        // 🔁 Fetch from /api/me
        if (!token.isNullOrEmpty()) {
            val request = Request.Builder()
                .url("$serverUrl/api/me")
                .addHeader("Authorization", token)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "")

                    val email = json.optString("email", "Nenurodytas")
                    val devices = json.optJSONArray("devices")
                    val deviceId = if (devices != null && devices.length() > 0) {
                        devices.getString(0)
                    } else {
                        "Nėra prijungto įrenginio"
                    }

                    withContext(Dispatchers.Main) {
                        emailText.text = "El. paštas: $email"
                        deviceText.text = "Įrenginys: $deviceId"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 🔌 Logout
        logoutButton.setOnClickListener {
            sharedPrefs.edit().remove("jwt").apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
