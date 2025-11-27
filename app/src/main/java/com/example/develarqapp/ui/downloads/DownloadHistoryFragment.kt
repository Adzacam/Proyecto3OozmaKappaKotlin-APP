package com.example.develarqapp.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentDownloadHistoryBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager

class DownloadHistoryFragment : Fragment() {

    private var _binding: FragmentDownloadHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private val viewModel: DownloadHistoryViewModel by viewModels()
    private lateinit var adapter: DownloadHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadHistoryBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupSearch()

        // Cargar datos
        viewModel.loadDownloadHistory()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role == "admin"
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupRecyclerView() {
        adapter = DownloadHistoryAdapter()
        binding.rvDownloads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DownloadHistoryFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                resources.getColor(R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_green_light, null),
                resources.getColor(android.R.color.holo_orange_light, null)
            )

            setOnRefreshListener {
                viewModel.loadDownloadHistory()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text.toString().trim()
            viewModel.filterDownloads(searchQuery = query)
        }
    }

    private fun setupObservers() {
        viewModel.filteredDownloads.observe(viewLifecycleOwner) { downloads ->
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(downloads)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.rvDownloads.isVisible = !isEmpty
            binding.tvPlaceholder.isVisible = isEmpty
            binding.cvInfo.isVisible = isEmpty
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                // Opcional: mostrar otro indicador
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}