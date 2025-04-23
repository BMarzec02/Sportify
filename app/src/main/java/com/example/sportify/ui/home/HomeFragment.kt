package com.example.sportify.ui.home

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sportify.R
import com.example.sportify.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var isRunning = false
    private var pauseOffset: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ViewModel współdzielony z DashboardFragment
        val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        // ViewBinding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // Tekst z ViewModel
        homeViewModel.text.observe(viewLifecycleOwner) {
            binding.textHome.text = it
        }

        // Referencje do przycisków
        val btnPlayPause: ImageButton = binding.buttonPlayPause
        val btnSave: ImageButton = binding.buttonSave

        // Spinner do wyboru aktywności
        val spinner: Spinner = binding.spinnerActivity
        val activities = listOf("Rower", "Bieganie", "Siłownia", "Inna")
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
                // Nie ustawiaj aktywności, jeśli nic nie wybrano
            }
        }

        btnPlayPause.setOnClickListener {
            if (!isRunning) {
                // START / WZNOWIENIE
                binding.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
                binding.chronometer.start()
                isRunning = true

                // Zmień ikonę na Pauza, kolor na niebieski i pokaż zapisz
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                btnPlayPause.backgroundTintList = requireContext().getColorStateList(R.color.blue)
                btnSave.visibility = View.VISIBLE

            } else {
                // PAUZA
                binding.chronometer.stop()
                pauseOffset = SystemClock.elapsedRealtime() - binding.chronometer.base
                isRunning = false

                // Zmień ikonę na Play, kolor na zielony
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
                btnPlayPause.backgroundTintList = requireContext().getColorStateList(R.color.green)
            }
        }

        btnSave.setOnClickListener {
            // Pobierz czas z chronometru i zapisz w ViewModel
            val time = binding.chronometer.text.toString()
            homeViewModel.addTime(time)
            Toast.makeText(requireContext(), "Zapisano czas: $time", Toast.LENGTH_SHORT).show()

            // Resetuj stoper i przyciski
            binding.chronometer.stop()
            binding.chronometer.base = SystemClock.elapsedRealtime()
            pauseOffset = 0L
            isRunning = false
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
            btnPlayPause.backgroundTintList = requireContext().getColorStateList(R.color.green)
            btnSave.visibility = View.GONE
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
