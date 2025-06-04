package com.example.sportify.ui.training

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sportify.R
import com.example.sportify.databinding.FragmentTrainingBinding
import com.google.android.gms.location.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    private var isRunning = false
    private var pauseOffset: Long = 0L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationList = mutableListOf<Location>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        val root = binding.root

        val btnPlayPause: ImageButton = binding.buttonPlayPause
        val btnSave: ImageButton = binding.buttonSave

        val spinner: Spinner = binding.spinnerActivity
        val activities = listOf(
            getString(R.string.transport_bike),
            getString(R.string.transport_running),
            getString(R.string.transport_other)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, activities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedActivity = activities[position]
                homeViewModel.setActivity(selectedActivity)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                homeViewModel.setActivity(getString(R.string.transport_other))
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationList.addAll(locationResult.locations)
            }
        }

        val mapViewMini = binding.mapViewMini
        mapViewMini.visibility = View.GONE
        mapViewMini.setMultiTouchControls(true)
        mapViewMini.controller.setZoom(18.0)

        // Ustaw domyślną pozycję (np. Warszawa)
        val startPoint = org.osmdroid.util.GeoPoint(52.2297, 21.0122)
        mapViewMini.controller.setCenter(startPoint)

        // marker lokalizacji użytkownika
        val userMarker = org.osmdroid.views.overlay.Marker(mapViewMini)
        userMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
        userMarker.icon = resources.getDrawable(R.drawable.ic_arrow, requireContext().theme)
        mapViewMini.overlays.add(userMarker)

        // aktualizacja markera przy każdej zmianie lokalizacji
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationList.addAll(locationResult.locations)
                val lastLocation = locationResult.lastLocation
                if (lastLocation != null) {
                    val point = org.osmdroid.util.GeoPoint(lastLocation.latitude, lastLocation.longitude)
                    userMarker.position = point
                    userMarker.rotation = lastLocation.bearing
                    mapViewMini.controller.setCenter(point)
                    mapViewMini.invalidate()
                }
            }
        }

        btnPlayPause.setOnClickListener {
            if (!isRunning) {
                // START / WZNOWIENIE
                binding.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
                binding.chronometer.start()
                isRunning = true

                // Pokaż mapę po kliknięciu START
                mapViewMini.visibility = View.VISIBLE

                // Zmień ikonę na Pauza, kolor na niebieski i pokaż zapisz
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                btnPlayPause.backgroundTintList = requireContext().getColorStateList(R.color.blue)
                btnSave.visibility = View.VISIBLE

                startLocationUpdates()
            } else {
                // PAUZA
                binding.chronometer.stop()
                pauseOffset = SystemClock.elapsedRealtime() - binding.chronometer.base
                isRunning = false
                stopLocationUpdates()
            }
        }

        btnSave.setOnClickListener {
            // Pobierz czas z chronometru i zapisz w ViewModel
            val time = binding.chronometer.text.toString()
            val gpxPath = saveGpxFile()
            val distance = String.format(Locale.US, "%.2f", calculateDistanceFromGpx(gpxPath))
            homeViewModel.addTrainingWithGpx(requireContext(), time, distance, gpxPath)

            Toast.makeText(requireContext(), "Zapisano aktywność: $time, $distance km", Toast.LENGTH_SHORT).show()

            // Resetuj stoper i przyciski
            binding.chronometer.stop()
            binding.chronometer.base = SystemClock.elapsedRealtime()
            pauseOffset = 0L
            isRunning = false
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
            btnPlayPause.backgroundTintList = requireContext().getColorStateList(R.color.green)
            btnSave.visibility = View.GONE
            locationList.clear()
        }

        return root
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveGpxFile(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "track_${sdf.format(Date())}.gpx"
        val file = File(requireContext().filesDir, fileName)
        val gpx = buildGpx(locationList)
        FileOutputStream(file).use { it.write(gpx.toByteArray()) }
        return file.absolutePath
    }

    private fun buildGpx(locations: List<Location>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Sportify\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n    <n>Trasa</n>\n    <trkseg>\n")
        for (loc in locations) {
            sb.append("      <trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\">\n")
            sb.append("        <ele>${loc.altitude}</ele>\n")
            sb.append("        <time>${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(loc.time))}</time>\n")
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>")
        return sb.toString()
    }

    private fun calculateDistanceFromGpx(gpxFilePath: String): Double {
        try {
            val points = mutableListOf<Pair<Double, Double>>()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            val file = File(gpxFilePath)
            parser.setInput(file.inputStream(), null)
            var eventType = parser.eventType
            var lat: Double? = null
            var lon: Double? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                if (eventType == XmlPullParser.START_TAG && tagName == "trkpt") {
                    lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                    lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        points.add(Pair(lat, lon))
                    }
                }
                eventType = parser.next()
            }
            var distance = 0.0
            for (i in 1 until points.size) {
                distance += haversine(points[i-1].first, points[i-1].second, points[i].first, points[i].second)
            }
            return distance
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
