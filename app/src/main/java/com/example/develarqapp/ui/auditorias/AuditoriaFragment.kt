package com.example.develarqapp.ui.auditorias

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentAuditoriaBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import java.text.SimpleDateFormat
import java.util.*

class AuditoriaFragment : Fragment() {

    private var _binding: FragmentAuditoriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuditoriaViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var auditoriaAdapter: AuditoriaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuditoriaBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar acceso - solo admin
        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        setupTopBar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        observeViewModel()

        // Cargar datos iniciales
        loadData()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role == "admin"
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    private fun setupRecyclerView() {
        auditoriaAdapter = AuditoriaAdapter()

        binding.rvAuditLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = auditoriaAdapter
            setHasFixedSize(true)
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
                loadData()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.filterLogs(text.toString())
        }
    }

    private fun loadData() {
        val token = sessionManager.getToken()
        if (token != null) {
            viewModel.loadAuditLogs(token)
        } else {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.filteredLogs.observe(viewLifecycleOwner) { logs ->
            binding.swipeRefresh.isRefreshing = false
            auditoriaAdapter.submitList(logs)

            // ✅ Actualizar estadísticas
            updateStatistics(logs)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.rvAuditLogs.isVisible = !isEmpty
            binding.llEmptyState.isVisible = isEmpty
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing && isLoading) {
                // Opcional: mostrar otro indicador si es la primera carga
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

    /**
     * ✅ Actualiza las estadísticas de auditoría
     */
    private fun updateStatistics(logs: List<com.example.develarqapp.data.model.AuditoriaLog>) {
        // Total
        binding.tvTotalCount.text = logs.size.toString()

        // Hoy
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val todayCount = logs.count { it.fecha.startsWith(today) }
        binding.tvTodayCount.text = todayCount.toString()

        // Esta semana
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())

        val weekCount = logs.count { log ->
            try {
                val logDate = dateFormat.parse(log.fecha)
                logDate != null && logDate.after(weekAgo)
            } catch (e: Exception) {
                false
            }
        }
        binding.tvWeekCount.text = weekCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}