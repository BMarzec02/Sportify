package com.example.sportify.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.sportify.R
import com.example.sportify.databinding.FragmentActivitiesBinding
import com.example.sportify.ui.training.HomeViewModel

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Pobierz ViewModel współdzielony z TrainingFragment
        val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        // Obserwuj zmiany w zapisanych czasach
        homeViewModel.savedTimes.observe(viewLifecycleOwner) { times ->
            val gpxList = homeViewModel.gpxPaths.value.orEmpty()
            if (times.isNotEmpty()) {
                // Wyciągnij czas i dystans z każdego rekordu
                val displayList = times.map { record ->
                    val parts = record.split("|")
                    val time = parts.getOrNull(0)?.trim() ?: ""
                    val distance = parts.getOrNull(1)?.trim() ?: ""
                    val date = parts.getOrNull(2)?.trim() ?: ""
                    val activityKey = parts.getOrNull(3)?.trim() ?: "other"

                    // Tłumaczenie rodzaju aktywności zgodnie z językiem
                    val activityName = when(activityKey) {
                        "Rower", "Bike" -> getString(R.string.transport_bike)
                        "Bieganie", "Running" -> getString(R.string.transport_running)
                        "Inne", "Other" -> getString(R.string.transport_other)
                        else -> activityKey
                    }

                    if (distance.isNotEmpty())
                        "$time   ($distance)\n$activityName, $date"
                    else
                        "$time\n$activityName, $date"
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
                binding.listViewTimes.adapter = adapter
                binding.listViewTimes.setOnItemClickListener { _, _, position, _ ->
                    val gpxPath = gpxList.getOrNull(position)
                    val activityInfo = times.getOrNull(position)
                    if (gpxPath != null) {
                        val bundle = Bundle().apply {
                            putString("gpxPath", gpxPath)
                            putString("activityInfo", activityInfo)
                        }
                        findNavController().navigate(
                            R.id.mapFragment,
                            bundle
                        )
                    }
                }

                // Obsługa długiego kliknięcia na element listy - usuwanie aktywności
                binding.listViewTimes.setOnItemLongClickListener { _, _, position, _ ->
                    val context = requireContext()
                    val builder = android.app.AlertDialog.Builder(context)
                    builder.setTitle("Usuń aktywność")
                    builder.setMessage("Czy na pewno chcesz usunąć tę aktywność?")
                    builder.setPositiveButton("Usuń") { _, _ ->
                        // Usuń z ViewModelu
                        val updatedTimes = homeViewModel.savedTimes.value?.toMutableList() ?: mutableListOf()
                        val updatedGpx = homeViewModel.gpxPaths.value?.toMutableList() ?: mutableListOf()
                        updatedTimes.removeAt(position)
                        if (position < updatedGpx.size) updatedGpx.removeAt(position)
                        homeViewModel.updateActivities(context, updatedTimes, updatedGpx)
                    }
                    builder.setNegativeButton("Anuluj", null)
                    builder.show()
                    true
                }
            } else {
                binding.listViewTimes.adapter = null // Wyczyść listę, jeśli brak danych
            }
        }

        // Obserwuj zmiany w liście GPX, aby odświeżać adapter
        homeViewModel.gpxPaths.observe(viewLifecycleOwner) {
            homeViewModel.savedTimes.value?.let { times ->
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, times)
                binding.listViewTimes.adapter = adapter
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
