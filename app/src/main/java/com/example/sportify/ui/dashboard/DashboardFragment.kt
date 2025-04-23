package com.example.sportify.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sportify.databinding.FragmentDashboardBinding
import com.example.sportify.ui.home.HomeViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Pobierz ViewModel współdzielony z HomeFragment
        val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        // Obserwuj zmiany w zapisanych czasach
        homeViewModel.savedTimes.observe(viewLifecycleOwner) { times ->
            if (times.isNotEmpty()) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, times)
                binding.listViewTimes.adapter = adapter
            } else {
                binding.listViewTimes.adapter = null // Wyczyść listę, jeśli brak danych
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
