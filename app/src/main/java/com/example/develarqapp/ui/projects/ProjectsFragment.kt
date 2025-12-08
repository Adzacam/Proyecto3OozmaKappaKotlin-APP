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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.FragmentProjectsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import androidx.core.content.ContextCompat

class ProjectsFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var projectsAdapter: ProjectsAdapter

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

        setupTopBar()
        setupRecyclerView()
        setupUI()
        setupSwipeRefresh()
        observeViewModel()

        // Cargar proyectos
        viewModel.loadProjects()
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupRecyclerView() {
        projectsAdapter = ProjectsAdapter(
            onEditClick = { project ->
                showEditProjectDialog(project)
            },
            onVersionsClick = { project ->
                navigateToTimeline(project)
            },
            onHitosClick = { project ->
                navigateToHitos(project)
            },
            onPermissionsClick = { project ->
                showPermissionsDialog(project)
            }
        )

        binding.rvProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectsAdapter
        }
    }

    private fun setupUI() {
        binding.btnNewProject.setOnClickListener {
            showCreateProjectDialog()
        }

        // Filtro de estado
        val estados = arrayOf("Todos", "Activo", "En Progreso", "Finalizado")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = adapter

        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Filtro local o VM
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.primary_green),
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
            setProgressViewOffset(false, 0, 200)
            setOnRefreshListener {
                viewModel.loadProjects()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            binding.swipeRefresh.isRefreshing = false

            if (projects.isEmpty()) {
                // binding.layoutEmptyState.isVisible = true
                // binding.rvProjects.isVisible = false
            } else {
                // binding.layoutEmptyState.isVisible = false
                binding.rvProjects.isVisible = true
                projectsAdapter.submitList(projects)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.isVisible = isLoading
            } else if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    // ============================================
    // DIÁLOGOS
    // ============================================

    private fun showCreateProjectDialog() {
        val dialog = CreateProjectDialog()
        dialog.show(childFragmentManager, "CreateProjectDialog")
    }

    private fun showEditProjectDialog(project: Project) {
        val dialog = EditProjectDialog.newInstance(
            projectId = project.id,
            projectNombre = project.nombre,
            projectDescripcion = project.descripcion ?: "",
            projectEstado = project.estado,
            projectFechaInicio = project.fechaInicio ?: "",
            projectFechaFin = project.fechaFin,
            projectClienteId = project.clienteId ?: 0,
            projectResponsableId = project.responsableId ?: 0
        )
        dialog.show(childFragmentManager, "EditProjectDialog")
    }

    private fun showPermissionsDialog(project: Project) {
        val dialog = PermissionsDialog.newInstance(project.id)
        dialog.show(childFragmentManager, "PermissionsDialog")
    }

    // ============================================
    // NAVEGACIÓN A FRAGMENT
    // ============================================

    private fun navigateToTimeline(project: Project) {
        val bundle = Bundle().apply {
            putLong("projectId", project.id)
            putString("projectName", project.nombre)
        }
        try {
            findNavController().navigate(
                R.id.action_projectsFragment_to_projectTimelineFragment,
                bundle
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHitos(project: Project) {
        val bundle = Bundle().apply {
            putLong("projectId", project.id)
            putString("projectName", project.nombre)
        }
        try {
            findNavController().navigate(
                R.id.action_projectsFragment_to_projectHitosFragment,
                bundle
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}