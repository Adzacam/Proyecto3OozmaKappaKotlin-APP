package com.example.develarqapp.ui.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.FragmentProjectsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.SimpleOnItemSelectedListener
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProjectsFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    private val viewModel: ProyectosViewModel by viewModels()
    private lateinit var adapter: ProjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        topBarManager.setupTopBar(binding.topAppBar.root)
        setupUI()
        setupObservers()
        
        // Cargar proyectos
        viewModel.loadProjects()
    }

    private fun setupUI() {
        // Spinner de filtro
        setupFilterSpinner()

        // RecyclerView
        setupRecyclerView()
    }

    private fun setupFilterSpinner() {
        val opciones = listOf("todos", "activo", "en progreso", "finalizado")
        
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            opciones
        )
        
        binding.spinnerFilter.adapter = spinnerAdapter
        binding.spinnerFilter.setSelection(0)
        
        binding.spinnerFilter.onItemSelectedListener = SimpleOnItemSelectedListener { position ->
            val filtroSeleccionado = opciones[position]
            viewModel.setFilter(filtroSeleccionado)
        }
    }

    private fun setupRecyclerView() {
        adapter = ProjectsAdapter(
            onEstadoChange = { proyecto, nuevoEstado ->
                confirmChangeState(proyecto, nuevoEstado)
            }
        )

        binding.rvProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProjectsFragment.adapter
        }
    }

    private fun setupObservers() {
        // Proyectos filtrados
        viewModel.filteredProjects.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
            
            // Mostrar/ocultar empty state
            binding.llEmptyState.isVisible = lista.isEmpty()
            binding.rvProjects.isVisible = lista.isNotEmpty()
        }

        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        // Error
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Success
        viewModel.operationSuccess.observe(viewLifecycleOwner) { successMessage ->
            successMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    /**
     * Confirmar cambio de estado con un diálogo
     */
    private fun confirmChangeState(proyecto: Project, nuevoEstado: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar estado")
            .setMessage("¿Seguro que deseas cambiar el estado de \"${proyecto.nombre}\" a \"$nuevoEstado\"?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.changeProjectState(proyecto.id, nuevoEstado)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}