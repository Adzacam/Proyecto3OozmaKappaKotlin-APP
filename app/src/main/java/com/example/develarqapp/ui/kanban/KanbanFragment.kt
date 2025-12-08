package com.example.develarqapp.ui.kanban

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentKanbanBinding
import com.example.develarqapp.data.model.ProjectSpinnerItem
import com.example.develarqapp.data.model.UserSpinnerItem
import com.example.develarqapp.data.model.TaskComplete
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class KanbanFragment : Fragment() {

    private var _binding: FragmentKanbanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private val viewModel: KanbanViewModel by viewModels()

    // Adapters para las 3 columnas Kanban
    private lateinit var pendienteAdapter: KanbanTaskAdapter
    private lateinit var enProgresoAdapter: KanbanTaskAdapter
    private lateinit var completadoAdapter: KanbanTaskAdapter

    // IDs seleccionados
    private var selectedProjectId: Long? = null
    private var selectedUserId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKanbanBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        setupRecyclerViews()
        setupSpinners()
        setupObservers()
        setupFAB()

        // Cargar datos iniciales
        viewModel.loadProjects()
        viewModel.loadUsers()
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupRecyclerViews() {
        // Adapter común con callbacks
        val onTaskClick: (TaskComplete) -> Unit = { task ->
            showTaskDetailDialog(task)
        }

        val onTaskLongClick: (TaskComplete) -> Boolean = { task ->
            showTaskOptionsDialog(task)
            true
        }

        // Columna PENDIENTE
        pendienteAdapter = KanbanTaskAdapter(onTaskClick, onTaskLongClick)
        binding.rvTasksPendiente.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendienteAdapter
        }

        // Columna EN PROGRESO
        enProgresoAdapter = KanbanTaskAdapter(onTaskClick, onTaskLongClick)
        binding.rvTasksEnProgreso.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = enProgresoAdapter
        }

        // Columna COMPLETADO
        completadoAdapter = KanbanTaskAdapter(onTaskClick, onTaskLongClick)
        binding.rvTasksCompletado.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completadoAdapter
        }
    }

    private fun setupSpinners() {
        // ============================================
        // SPINNER DE PROYECTOS
        // ============================================
        binding.spinnerProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val project = parent?.getItemAtPosition(position) as ProjectSpinnerItem
                    selectedProjectId = project.id
                    viewModel.setSelectedProject(project.id)

                    binding.tvEmptyKanban.visibility = View.GONE
                    binding.hsvKanban.visibility = View.VISIBLE
                    binding.fabCreateTask.visibility = View.VISIBLE
                } else {
                    selectedProjectId = null
                    viewModel.setSelectedProject(null)

                    binding.tvEmptyKanban.visibility = View.VISIBLE
                    binding.tvEmptyKanban.text = "Selecciona un proyecto para ver su tablero."
                    binding.hsvKanban.visibility = View.GONE
                    binding.fabCreateTask.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ============================================
        // SPINNER DE RESPONSABLES (FILTRO)
        // ============================================
        binding.spinnerResponsible.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedUserId = if (position > 0) {
                    val user = parent?.getItemAtPosition(position) as UserSpinnerItem
                    user.id
                } else {
                    null
                }
                viewModel.setSelectedUser(selectedUserId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        // ============================================
        // OBSERVAR PROYECTOS
        // ============================================
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            val items = mutableListOf<ProjectSpinnerItem>()
            items.add(ProjectSpinnerItem(0, "Seleccionar proyecto"))
            items.addAll(projects.map { ProjectSpinnerItem(it.id, it.nombre) })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerProject.adapter = adapter
        }

        // ============================================
        // OBSERVAR USUARIOS
        // ============================================
        viewModel.users.observe(viewLifecycleOwner) { users ->
            val items = mutableListOf<UserSpinnerItem>()
            items.add(UserSpinnerItem(0, "Todos"))
            items.addAll(users.map {
                UserSpinnerItem(it.id, it.fullName)
            })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerResponsible.adapter = adapter
        }

        // ============================================
        // OBSERVAR COLUMNAS KANBAN
        // ============================================
        viewModel.kanbanColumns.observe(viewLifecycleOwner) { columns ->
            columns.forEach { column ->
                when (column.estado.lowercase()) {
                    "pendiente" -> {
                        pendienteAdapter.submitList(column.tareas.toList())
                    }
                    "en progreso" -> {
                        enProgresoAdapter.submitList(column.tareas.toList())
                    }
                    "completado" -> {
                        completadoAdapter.submitList(column.tareas.toList())
                    }
                }
            }
        }

        // ============================================
        // OBSERVAR MENSAJES
        // ============================================
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // ============================================
        // OBSERVAR EMPTY STATE
        // ============================================
        viewModel.showEmptyState.observe(viewLifecycleOwner) { showEmpty ->

            if (showEmpty) {
                binding.tvEmptyKanban.visibility = View.VISIBLE
                binding.hsvKanban.visibility = View.GONE
                binding.fabCreateTask.visibility = View.GONE
            } else {

                binding.tvEmptyKanban.visibility = View.GONE
                binding.hsvKanban.visibility = View.VISIBLE
                binding.fabCreateTask.visibility = View.VISIBLE
            }
        }

        // ============================================
        // OBSERVAR LOADING
        // ============================================
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->

        }
    }

    private fun setupFAB() {
        binding.fabCreateTask.setOnClickListener {
            selectedProjectId?.let { projectId ->
                val dialog = CreateTaskDialog.newInstance(projectId)
                dialog.show(childFragmentManager, "CreateTaskDialog")
            } ?: run {
                Snackbar.make(binding.root, "Selecciona un proyecto primero", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ============================================
    // DIALOGS
    // ============================================

    private fun showTaskDetailDialog(task: TaskComplete) {
        val details = buildString {
            append("📋 ${task.titulo}\n\n")

            if (!task.descripcion.isNullOrEmpty()) {
                append("📄 Descripción:\n${task.descripcion}\n\n")
            }

            append("📊 Estado: ${task.estado.uppercase()}\n")
            append("⚡ Prioridad: ${task.prioridad.uppercase()}\n")

            if (!task.asignadoNombre.isNullOrEmpty()) {
                append("👤 Asignado: ${task.asignadoNombre}\n")
            } else {
                append("👤 Sin asignar\n")
            }

            if (!task.fechaLimite.isNullOrEmpty()) {
                append("📅 Fecha límite: ${formatDate(task.fechaLimite)}\n")
            }

            if (!task.proyectoNombre.isNullOrEmpty()) {
                append("🏗️ Proyecto: ${task.proyectoNombre}\n")
            }

            if (!task.creadorNombre.isNullOrEmpty()) {
                append("\n👨‍💼 Creado por: ${task.creadorNombre}\n")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detalles de la Tarea")
            .setMessage(details)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Editar") { _, _ ->
                val dialog = EditTaskDialog.newInstance(task.id)
                dialog.show(childFragmentManager, "EditTaskDialog")
            }
            .show()
    }

    private fun showTaskOptionsDialog(task: TaskComplete) {
        val options = arrayOf(
            "📋 Ver detalles",
            "✏️ Editar",
            "⏮️ Mover a Pendiente",
            "▶️ Mover a En Progreso",
            "✅ Mover a Completado",
            "🗑️ Eliminar"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(task.titulo)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTaskDetailDialog(task)
                    1 -> {
                        val dialog = EditTaskDialog.newInstance(task.id)
                        dialog.show(childFragmentManager, "EditTaskDialog")
                    }
                    2 -> viewModel.updateTaskState(task.id, "pendiente")
                    3 -> viewModel.updateTaskState(task.id, "en progreso")
                    4 -> viewModel.updateTaskState(task.id, "completado")
                    5 -> confirmDeleteTask(task)
                }
            }
            .show()
    }

    private fun confirmDeleteTask(task: TaskComplete) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Tarea")
            .setMessage("¿Estás seguro de que deseas eliminar '${task.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============================================
    // UTILS
    // ============================================

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}