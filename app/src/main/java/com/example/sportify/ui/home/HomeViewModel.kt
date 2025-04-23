package com.example.sportify.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private val _savedTimes = MutableLiveData<List<String>>(emptyList())
    val savedTimes: LiveData<List<String>> = _savedTimes

    private val _selectedActivity = MutableLiveData<String>("Inna")
    val selectedActivity: LiveData<String> = _selectedActivity

    fun setActivity(activity: String) {
        _selectedActivity.value = activity
    }

    fun addTime(time: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val activity = _selectedActivity.value ?: "Brak aktywności"
        val timeWithDate = "$time $currentDate - $activity"

        val updatedList = _savedTimes.value.orEmpty().toMutableList()
        updatedList.add(timeWithDate)
        _savedTimes.value = updatedList
    }
}

