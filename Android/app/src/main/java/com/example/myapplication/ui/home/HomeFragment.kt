package com.example.myapplication.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.widget.Button


class HomeFragment : Fragment() {

    private val client = OkHttpClient()
    private var mMap: GoogleMap? = null
    private var marker: Marker? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 60000L // 60 seconds

    private val fetchLocationRunnable = object : Runnable {
        override fun run() {
            fetchAndUpdateLocation()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            fetchAndUpdateLocation()
            handler.postDelayed(fetchLocationRunnable, refreshInterval)

            // Add Zoom Button Actions
            val zoomIn = view.findViewById<Button>(R.id.zoomInButton)
            val zoomOut = view.findViewById<Button>(R.id.zoomOutButton)

            zoomIn.setOnClickListener {
                mMap?.animateCamera(CameraUpdateFactory.zoomIn())
            }

            zoomOut.setOnClickListener {
                mMap?.animateCamera(CameraUpdateFactory.zoomOut())
            }
        }

        return view
    }


    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(fetchLocationRunnable)
    }

    private fun fetchAndUpdateLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("http://194.31.55.182:3001/api/location")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseData = response.body?.string()
                    if (response.isSuccessful && responseData != null) {
                        val json = JSONObject(responseData)
                        val lat = json.getDouble("lat")
                        val lon = json.getDouble("lon")
                        updateMap(lat, lon)
                    }
                }
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    private fun updateMap(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.Main).launch {
            val location = LatLng(lat, lon)
            if (marker == null) {
                marker = mMap?.addMarker(MarkerOptions().position(location).title("Latest Location"))
            } else {
                marker?.position = location
            }
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }
}
