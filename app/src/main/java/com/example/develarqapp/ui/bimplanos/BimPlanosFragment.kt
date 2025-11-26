package com.example.develarqapp.ui.bimplanos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.databinding.FragmentBimPlanosBinding
import com.example.develarqapp.ui.bimplans.BimPlanosAdapter
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager

class BimPlanosFragment : Fragment() {

    private var _binding: FragmentBimPlanosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var bimPlansAdapter: BimPlanosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBimPlanosBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar acceso según rol
        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupUI()
        setupRecyclerView()
        observeViewModel()

        // Cargar planos
        viewModel.loadBimPlans()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(com.example.develarqapp.R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    private fun setupUI() {
        // Botón subir plano
        binding.btnSubirPlano.setOnClickListener {
            // TODO: Abrir diálogo de subir plano
            Toast.makeText(requireContext(), "Subir Plano BIM - Por implementar", Toast.LENGTH_SHORT).show()
        }

        // Botón ver papelera
        binding.btnVerPapelera.setOnClickListener {
            // TODO: Navegar a papelera de planos
            Toast.makeText(requireContext(), "Ver Papelera - Por implementar", Toast.LENGTH_SHORT).show()
        }

        // Botón limpiar filtros
        binding.btnLimpiarFiltros.setOnClickListener {
            clearFilters()
        }

        // TODO: Configurar spinners y date pickers
        setupFilters()
    }

    private fun setupFilters() {
        // TODO: Configurar spinners de tipo, proyecto y ordenamiento
        // TODO: Configurar date pickers para rango de fechas
        // TODO: Implementar búsqueda en tiempo real
    }

    private fun clearFilters() {
        binding.etSearch.text?.clear()
        binding.etDateFrom.text?.clear()
        binding.etDateTo.text?.clear()
        // TODO: Resetear spinners
        viewModel.loadBimPlans()
    }

    private fun setupRecyclerView() {
        bimPlansAdapter = BimPlanosAdapter(
            onDownloadClick = { plan ->
                viewModel.downloadBimPlan(plan.id)
                Toast.makeText(requireContext(), "Descargando: ${plan.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { plan ->
                // TODO: Abrir diálogo de edición
                Toast.makeText(requireContext(), "Editar: ${plan.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { plan ->
                // TODO: Mostrar diálogo de confirmación
                viewModel.deleteBimPlan(plan.id)
            }
        )

        binding.rvBimPlans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bimPlansAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.bimPlans.observe(viewLifecycleOwner) { plans ->
            if (plans.isEmpty()) {
                binding.tvEmptyState.isVisible = true
                binding.rvBimPlans.isVisible = false
            } else {
                binding.tvEmptyState.isVisible = false
                binding.rvBimPlans.isVisible = true
                bimPlansAdapter.submitList(plans)
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

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}