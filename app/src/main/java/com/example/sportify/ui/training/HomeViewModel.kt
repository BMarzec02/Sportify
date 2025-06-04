package com.example.sportify.ui.training

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// główny model przechowujący dane aplikacji
class HomeViewModel : ViewModel() {

    private val _savedTimes = MutableLiveData<List<String>>(emptyList())
    val savedTimes: LiveData<List<String>> = _savedTimes

    private val _selectedActivity = MutableLiveData<String>("Inna")
    val selectedActivity: LiveData<String> = _selectedActivity

    // całkowity dystans ze wszystkich treningów
    private val _totalDistance = MutableLiveData<Float>(0f)
    val totalDistance: LiveData<Float> = _totalDistance

    // ścieżki do plików gpx z trasami
    private val _gpxPaths = MutableLiveData<List<String>>(emptyList())
    val gpxPaths: LiveData<List<String>> = _gpxPaths

    // nazwy dla zapisywania danych w pamięci telefonu
    private val PREFS_NAME = "sportify_prefs"
    private val KEY_SAVED_TIMES = "saved_times"
    private val KEY_GPX_PATHS = "gpx_paths"

    // licznik aktywności - zlicza ile jest treningów
    val activityCount: LiveData<Int> = MutableLiveData<Int>().apply {
        this.value = _savedTimes.value?.size ?: 0
        _savedTimes.observeForever { list ->
            this.value = list.size
        }
    }

    // obliczanie całkowitego czasu wszystkich treningów
    val totalTime: LiveData<Long> = MutableLiveData<Long>().apply {
        // funkcja konwertująca czas z formatu tekstowego na milisekundy
        fun parseTimeToMillis(time: String): Long {
            val parts = time.split(":")
            return when (parts.size) {
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000
                else -> 0L
            }
        }
        // sumowanie wszystkich czasów
        fun sumTimes(list: List<String>): Long {
            return list.mapNotNull {
                val time = it.split("|").firstOrNull()?.trim()?.takeIf { t -> t.contains(":") }
                time?.let { parseTimeToMillis(it) }
            }.sum()
        }
        this.value = sumTimes(_savedTimes.value ?: emptyList())
        _savedTimes.observeForever { list ->
            this.value = sumTimes(list)
        }
    }

    init {
        // aktualizacja dystansu gdy zmieni się lista aktywności
        _savedTimes.observeForever { list ->
            _totalDistance.value = sumDistances(list)
        }
    }

    // funkcja sumująca dystanse ze wszystkich treningów
    private fun sumDistances(list: List<String>): Float {
        return list.mapNotNull { record ->
            val parts = record.split("|")
            val distanceStr = parts.getOrNull(1)?.replace("km", "")?.replace(",", ".")?.trim()
            distanceStr?.toFloatOrNull()
        }.sum()
    }

    // wczytywanie danych z pamięci telefonu
    fun loadData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val times = prefs.getStringSet(KEY_SAVED_TIMES, null)?.toList() ?: emptyList()
        val gpx = prefs.getStringSet(KEY_GPX_PATHS, null)?.toList() ?: emptyList()
        _savedTimes.value = times
        _gpxPaths.value = gpx
    }

    // zapisywanie danych do pamięci telefonu
    private fun saveData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_SAVED_TIMES, _savedTimes.value?.toSet() ?: emptySet())
            .putStringSet(KEY_GPX_PATHS, _gpxPaths.value?.toSet() ?: emptySet())
            .apply()
    }

    // ustawienie nowej aktywności (rower, bieganie, inne)
    fun setActivity(activityKey: String) {
        _selectedActivity.value = activityKey
    }

    // dodanie nowego czasu treningu bez dystansu
    fun addTime(context: Context, time: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val activityKey = _selectedActivity.value ?: "other"
        val timeWithDate = "$time | $currentDate | $activityKey"

        val updatedList = _savedTimes.value.orEmpty().toMutableList()
        updatedList.add(timeWithDate)
        _savedTimes.value = updatedList
        saveData(context)
    }

    // dodanie treningu z dystansem
    fun addTraining(context: Context, time: String, distance: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val activityKey = _selectedActivity.value ?: "other"
        val record = "$time | ${distance} km | $currentDate | $activityKey"

        val updatedList = _savedTimes.value.orEmpty().toMutableList()
        updatedList.add(record)
        _savedTimes.value = updatedList

        // aktualizacja całkowitego dystansu
        val dist = distance.toFloatOrNull() ?: 0f
        _totalDistance.value = (_totalDistance.value ?: 0f) + dist
        saveData(context)
    }

    // dodanie treningu z dystansem i plikiem gpx z trasą
    fun addTrainingWithGpx(context: Context, time: String, distance: String, gpxPath: String) {
        val currentDate = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val activityKey = _selectedActivity.value ?: "other"
        val record = "$time | ${distance} km | $currentDate | $activityKey"

        // aktualizacja listy treningów
        val updatedList = _savedTimes.value.orEmpty().toMutableList()
        updatedList.add(record)
        _savedTimes.value = updatedList

        // aktualizacja listy plików gpx
        val updatedGpx = _gpxPaths.value.orEmpty().toMutableList()
        updatedGpx.add(gpxPath)
        _gpxPaths.value = updatedGpx

        // aktualizacja całkowitego dystansu
        val dist = distance.toFloatOrNull() ?: 0f
        _totalDistance.value = (_totalDistance.value ?: 0f) + dist
        saveData(context)
    }

    // aktualizacja listy treningów i plików gpx
    fun updateActivities(context: Context, newTimes: List<String>, newGpx: List<String>) {
        _savedTimes.value = newTimes
        _gpxPaths.value = newGpx
        saveData(context)
    }
}
