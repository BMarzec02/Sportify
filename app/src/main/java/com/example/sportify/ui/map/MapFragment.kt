package com.example.sportify.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class MapFragment : Fragment() {
    private var mapView: MapView? = null

    // funkcja tworząca widok fragmentu mapy
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext().applicationContext
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
        val view = inflater.inflate(com.example.sportify.R.layout.fragment_map, container, false)
        mapView = view.findViewById(com.example.sportify.R.id.mapView)
        mapView?.setMultiTouchControls(true)
        mapView?.controller?.setZoom(15.0)
        mapView?.controller?.setCenter(GeoPoint(52.2297, 21.0122))


        // pobierz ścieżkę do pliku GPX
        val gpxPath = arguments?.getString("gpxPath")
        if (gpxPath != null) {
            drawGpxTrack(gpxPath)
        }
        return view
    }

    // funkcja wywoływana po utworzeniu widoku, pokazuje informacje o aktywności
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gpxPath = arguments?.getString("gpxPath")
        val timeTextView = view.findViewById<android.widget.TextView>(com.example.sportify.R.id.textActivityTime)
        val distanceTextView = view.findViewById<android.widget.TextView>(com.example.sportify.R.id.textActivityDistance)
        val avgSpeedTextView = view.findViewById<android.widget.TextView>(com.example.sportify.R.id.textActivityAvgSpeed)

        // Pobierz czas i dystans z argumentów lub GPX
        val activityInfo = arguments?.getString("activityInfo")
        var timeStr = "--:--:--"
        var distanceStr = "--"
        if (activityInfo != null) {
            val parts = activityInfo.split("|")
            timeStr = parts.getOrNull(0)?.trim() ?: timeStr
            distanceStr = parts.getOrNull(1)?.replace("km", "")?.trim() ?: distanceStr
        }
        // Jeśli nie ma dystansu w argumentach, policz z GPX
        var distance = distanceStr.toDoubleOrNull()
        if ((distance == null || distance == 0.0) && gpxPath != null) {
            distance = calculateDistanceFromGpx(gpxPath)
            distanceStr = String.format("%.2f", distance)
        }
        timeTextView.text = "${getString(com.example.sportify.R.string.activity_time)}: $timeStr"
        distanceTextView.text = "${getString(com.example.sportify.R.string.activity_distance)}: $distanceStr km"
        // Oblicz średnią prędkość
        val avgSpeed = calculateAvgSpeed(timeStr, distance ?: 0.0)
        avgSpeedTextView.text = "${getString(com.example.sportify.R.string.activity_avg_speed)}: $avgSpeed km/h"
    }

    // funkcja rysująca trasę na mapie z pliku gpx
    private fun drawGpxTrack(gpxPath: String) {
        val file = File(gpxPath)
        if (!file.exists()) return
        val points = mutableListOf<GeoPoint>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(file.inputStream(), null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "trkpt") {
                    val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                    val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        points.add(GeoPoint(lat, lon))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (points.isNotEmpty()) {
            val polyline = Polyline()
            polyline.setPoints(points)
            mapView?.overlays?.add(polyline)

            // Tworzenie  wokół wszystkich punktów trasy
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.minOf { it.longitude }

            // Utworzenie  zmarginesem 10%
            val latSpan = maxLat - minLat
            val lonSpan = maxLon - minLon
            val boundingBox = org.osmdroid.util.BoundingBox(
                maxLat + latSpan * 0.1,
                maxLon + lonSpan * 0.1,
                minLat - latSpan * 0.1,
                minLon - lonSpan * 0.1
            )

            // Ustawienie widoku mapy tak, aby pokazać cala trasę
            mapView?.let {
                it.post {
                    it.zoomToBoundingBox(boundingBox, true)
                    it.invalidate()
                }
            }
        }
    }

    // funkcja obliczająca dystans z pliku gpx
    private fun calculateDistanceFromGpx(gpxPath: String): Double {
        try {
            val points = mutableListOf<Pair<Double, Double>>()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            val file = File(gpxPath)
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

    // funkcja licząca odległość między dwoma punktami na ziemi
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

    // funkcja obliczająca średnią prędkość na podstawie czasu i dystansu
    private fun calculateAvgSpeed(timeStr: String, distance: Double): String {
        val parts = timeStr.split(":")
        val seconds = when (parts.size) {
            //minuty
            2 -> parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
            //godziny
            3 -> parts[0].toIntOrNull()?.times(3600)?.plus(parts[1].toIntOrNull()?.times(60) ?: 0)?.plus(parts[2].toIntOrNull() ?: 0) ?: 0
            else -> 0
        }
        if (seconds == 0) return "--"
        val hours = seconds / 3600.0
        if (hours == 0.0) return "--"
        val avg = distance / hours
        return String.format("%.2f", avg)
    }

    // funkcja wywoływana przy wznowieniu aktywności fragmentu
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    // funkcja wywoływana przy wstrzymaniu aktywności fragmentu
    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    // funkcja wywoływana przy niszczeniu widoku, zwalnia zasoby mapy
    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDetach()
    }
}

