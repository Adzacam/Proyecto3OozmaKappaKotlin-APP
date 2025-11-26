package com.example.develarqapp.ui.auditorias

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.databinding.FragmentAuditoriaBinding
import com.example.develarqapp.ui.auditorias.AuditoriaAdapter
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager

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
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupUI()
        setupRecyclerView()
        observeViewModel()


        val token = sessionManager.getToken()
        if (token != null) {
            viewModel.loadAuditLogs(token)
        } else {

            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed() // Salir si no hay token
        }
    }
    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role == "admin"
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(com.example.develarqapp.R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    private fun setupUI() {
        // Botón limpiar filtros
        binding.btnLimpiarFiltros.setOnClickListener {
            clearFilters()
        }

        // TODO: Configurar spinners y date pickers
        setupFilters()
    }

    private fun setupFilters() {
        // TODO: Configurar spinner de tipos de acción
        // TODO: Configurar date pickers para rango de fechas
        // TODO: Implementar búsqueda en tiempo real
        // TODO: Filtro por usuario
    }

    private fun clearFilters() {
        binding.etSearchAction.text?.clear()
        binding.etFilterUser.text?.clear()
        binding.etDateFrom.text?.clear()
        binding.etDateTo.text?.clear()
        // TODO: Resetear spinners


        val token = sessionManager.getToken()
        if (token != null) {
            viewModel.loadAuditLogs(token)
        } else {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        auditoriaAdapter = AuditoriaAdapter()

        binding.rvAuditLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = auditoriaAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.auditLogs.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) {
                binding.tvEmptyState.isVisible = true
                binding.rvAuditLogs.isVisible = false
            } else {
                binding.tvEmptyState.isVisible = false
                binding.rvAuditLogs.isVisible = true
                auditoriaAdapter.submitList(logs)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}