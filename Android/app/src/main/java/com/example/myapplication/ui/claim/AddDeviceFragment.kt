package com.example.myapplication.ui.claim

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AddDeviceFragment : Fragment() {

    private val serverUrl = "http://194.31.55.182:3001"
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_device, container, false)

        val claimInput = view.findViewById<EditText>(R.id.claimCodeInput)
        val claimButton = view.findViewById<Button>(R.id.claimButton)
        val unclaimButton = view.findViewById<Button>(R.id.unclaimButton)
        val deviceInfo = view.findViewById<TextView>(R.id.deviceInfoText)

        val jwt = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt", null)

        if (jwt == null) {
            Toast.makeText(context, "Neprisijungęs naudotojas", Toast.LENGTH_SHORT).show()
            return view
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val meRequest = Request.Builder()
                    .url("$serverUrl/api/me")
                    .addHeader("Authorization", jwt)
                    .build()

                val response = client.newCall(meRequest).execute()
                val json = JSONObject(response.body?.string() ?: "")
                val devices = json.getJSONArray("devices")

                withContext(Dispatchers.Main) {
                    if (devices.length() == 0) {
                        claimInput.visibility = View.VISIBLE
                        claimButton.visibility = View.VISIBLE
                        unclaimButton.visibility = View.GONE
                        deviceInfo.text = "Joks prietaisas nepridėtas"
                    } else {
                        val deviceId = devices.getString(0)
                        deviceInfo.text = "Prietaisas: $deviceId"
                        claimInput.visibility = View.GONE
                        claimButton.visibility = View.GONE
                        unclaimButton.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Klaida tikrinant prietaisą", Toast.LENGTH_SHORT).show()
                }
            }
        }


        claimButton.setOnClickListener {
            val code = claimInput.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(context, "Įveskite priskyrimo kodą", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject().put("claimCode", code)
            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$serverUrl/api/claim")
                .post(body)
                .addHeader("Authorization", jwt)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Prietaisas pridėtas", Toast.LENGTH_SHORT).show()
                        requireActivity().recreate() // restart to re-evaluate navigation
                    } else {
                        Toast.makeText(context, "Klaida: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        unclaimButton.setOnClickListener {
            val request = Request.Builder()
                .url("$serverUrl/api/unclaim")
                .post("".toRequestBody()) // empty POST body
                .addHeader("Authorization", jwt)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Prietaisas pašalintas", Toast.LENGTH_SHORT).show()
                        requireActivity().recreate()
                    } else {
                        Toast.makeText(context, "Nepavyko pašalinti: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }
}
