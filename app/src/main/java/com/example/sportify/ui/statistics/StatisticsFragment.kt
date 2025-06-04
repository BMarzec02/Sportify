package com.example.sportify.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sportify.R
import com.example.sportify.databinding.FragmentStatisticsBinding
import com.example.sportify.ui.training.HomeViewModel

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val statsView: TextView = binding.textStats

        // Obserwuj statystyki i wyświetlaj je
        homeViewModel.activityCount.observe(viewLifecycleOwner) { count ->
            val totalTime = homeViewModel.totalTime.value ?: 0L
            val totalDistance = homeViewModel.totalDistance.value ?: 0f
            val formattedTime = formatMillis(totalTime)
            statsView.text = "${getString(R.string.your_stats)}\n${getString(R.string.stats_activity_count)}: $count\n${getString(R.string.stats_total_time)}: $formattedTime\n${getString(R.string.stats_total_distance)}: %.2f km".format(totalDistance)
        }
        homeViewModel.totalTime.observe(viewLifecycleOwner) { totalTime ->
            val count = homeViewModel.activityCount.value ?: 0
            val totalDistance = homeViewModel.totalDistance.value ?: 0f
            val formattedTime = formatMillis(totalTime)
            statsView.text = "${getString(R.string.your_stats)}\n${getString(R.string.stats_activity_count)}: $count\n${getString(R.string.stats_total_time)}: $formattedTime\n${getString(R.string.stats_total_distance)}: %.2f km".format(totalDistance)
        }
        homeViewModel.totalDistance.observe(viewLifecycleOwner) { totalDistance ->
            val count = homeViewModel.activityCount.value ?: 0
            val totalTime = homeViewModel.totalTime.value ?: 0L
            val formattedTime = formatMillis(totalTime)
            statsView.text = "${getString(R.string.your_stats)}\n${getString(R.string.stats_activity_count)}: $count\n${getString(R.string.stats_total_time)}: $formattedTime\n${getString(R.string.stats_total_distance)}: %.2f km".format(totalDistance)
        }

        return root
    }

    private fun formatMillis(millis: Long): String {
        val seconds = millis / 1000 % 60
        val minutes = millis / (1000 * 60) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
