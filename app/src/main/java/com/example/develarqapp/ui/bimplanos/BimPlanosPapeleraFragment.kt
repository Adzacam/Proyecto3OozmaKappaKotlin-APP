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
import com.example.develarqapp.databinding.FragmentBimPlanosPapeleraBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.develarqapp.R


class BimPlanosPapeleraFragment : Fragment() {

    private var _binding: FragmentBimPlanosPapeleraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var papeleraAdapter: BimPlanosPapeleraAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBimPlanosPapeleraBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar acceso (solo admin, ingeniero, arquitecto)
        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupRecyclerView()
        setupUI()
        observeViewModel()

        // Cargar planos eliminados
        viewModel.loadDeletedBimPlanos()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(com.example.develarqapp.R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    // ============================================
// ✅ REEMPLAZAR setupUI() COMPLETA
// ============================================
    private fun setupUI() {
        binding.btnRegresar.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // ✅ Configurar SwipeRefresh
        binding.swipeRefreshPapelera.apply {
            setColorSchemeColors(
                resources.getColor(R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_red_light, null)
            )
            setProgressViewOffset(false, 0, 200)

            setOnRefreshListener {
                viewModel.loadDeletedBimPlanos()
            }
        }
    }

    // ============================================
    //observeViewModel() COMPLETA
    // ============================================
        private fun observeViewModel() {
            viewModel.bimPlanosEliminados.observe(viewLifecycleOwner) { planos ->
                binding.swipeRefreshPapelera.isRefreshing = false

                if (planos.isEmpty()) {
                    binding.tvEmptyState.isVisible = true
                    binding.rvPapelera.isVisible = false
                } else {
                    binding.tvEmptyState.isVisible = false
                    binding.rvPapelera.isVisible = true
                    papeleraAdapter.submitList(planos)
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                if (!isLoading) {
                    binding.swipeRefreshPapelera.isRefreshing = false
                }
                binding.progressBar.isVisible = isLoading
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let {
                    binding.swipeRefreshPapelera.isRefreshing = false
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }

            viewModel.successMessage.observe(viewLifecycleOwner) { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                    // ✅ Recargar lista después de restaurar/eliminar
                    viewModel.loadDeletedBimPlanos()
                }
            }
        }

        private fun setupRecyclerView() {
            papeleraAdapter = BimPlanosPapeleraAdapter(
                onRestoreClick = { plano ->
                    showRestoreDialog(plano.id, plano.titulo)
                },
                onDeleteClick = { plano ->
                    showPermanentDeleteDialog(plano.id, plano.titulo)
                }
            )

            binding.rvPapelera.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = papeleraAdapter
            }
        }

    // ============================================
    // DIÁLOGOS DE CONFIRMACIÓN
    // ============================================
    private fun showRestoreDialog(planoId: Long, titulo: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restaurar Plano")
            .setMessage("¿Deseas restaurar el plano '$titulo'?")
            .setPositiveButton("Restaurar") { _, _ ->
                viewModel.restoreBimPlano(planoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPermanentDeleteDialog(planoId: Long, titulo: String) {
        val isAdmin = sessionManager.getUserRol().lowercase() == "admin"

        if (!isAdmin) {
            Toast.makeText(
                requireContext(),
                "Solo administradores pueden eliminar permanentemente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Eliminar Permanentemente")
            .setMessage("¿Estás seguro de eliminar permanentemente el plano '$titulo'?\n\n⚠️ Esta acción NO se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.permanentDeleteBimPlano(planoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun setupSwipeRefresh() {
        // Verificar que el SwipeRefreshLayout existe en el XML
        binding.root.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(
            com.example.develarqapp.R.id.swipeRefreshPapelera
        )?.apply {
            setColorSchemeColors(
                resources.getColor(com.example.develarqapp.R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_red_light, null)
            )
            setProgressViewOffset(false, 0, 200)

            setOnRefreshListener {
                viewModel.loadDeletedBimPlanos()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}