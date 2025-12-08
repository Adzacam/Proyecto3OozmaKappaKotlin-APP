package com.example.develarqapp.ui.kanban

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogEditTaskBinding
import com.example.develarqapp.data.model.TaskComplete
import com.example.develarqapp.data.model.UserSpinnerItem
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class EditTaskDialog : DialogFragment() {

    private var _binding: DialogEditTaskBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels para compartir datos con el fragmento padre
    private val viewModel: KanbanViewModel by activityViewModels()

    private var task: TaskComplete? = null
    private var selectedDate: String? = null
    private var taskId: Long = 0

    companion object {
        private const val ARG_TASK_ID = "task_id"

        fun newInstance(taskId: Long): EditTaskDialog {
            return EditTaskDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        taskId = arguments?.getLong(ARG_TASK_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cargar usuarios para el spinner
        viewModel.loadUsers()

        // 2. Buscar la tarea en el ViewModel
        findTaskAndPopulate()

        setupSpinners()
        setupDatePicker()
        setupButtons()
    }

    private fun findTaskAndPopulate() {
        // Observamos las columnas para encontrar la tarea
        viewModel.kanbanColumns.observe(viewLifecycleOwner) { columns ->
            var found = false
            for (column in columns) {
                val foundTask = column.tareas.find { it.id == taskId }
                if (foundTask != null) {
                    task = foundTask
                    populateFields(foundTask)
                    // Actualizar selección de spinners si ya se cargaron los usuarios
                    updateSpinnersSelection()
                    found = true
                    break
                }
            }
            if (!found && task == null) {
                Toast.makeText(context, "Error: No se encontró la tarea", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun populateFields(task: TaskComplete) {
        with(binding) {
            tvTaskCode.text = "TASK-${task.id.toString().padStart(3, '0')}"
            etTaskTitle.setText(task.titulo)
            etTaskDescription.setText(task.descripcion)

            // Fecha límite
            task.fechaLimite?.let {
                try {
                    val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = dbFormat.parse(it)
                    etDueDate.setText(date?.let { d -> displayFormat.format(d) })
                    selectedDate = it
                } catch (e: Exception) {
                    etDueDate.setText(it)
                }
            }

            tvCreatedBy.text = "Creado por: ${task.creadorNombre ?: "Desconocido"}"
            task.createdAt?.let { tvCreatedAt.text = "Fecha: ${formatDate(it)}" }
            task.updatedAt?.let { tvUpdatedAt.text = "Última modificación: ${formatDate(it)}" }
        }
    }

    private fun setupSpinners() {
        // Estado
        val estados = listOf("pendiente", "en progreso", "completado")
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            estados.map { it.uppercase() }
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        // Prioridad
        val prioridades = listOf("baja", "media", "alta")
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            prioridades.map { it.uppercase() }
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriority.adapter = priorityAdapter

        // Asignado (Observamos usuarios)
        viewModel.users.observe(viewLifecycleOwner) { users ->
            val items = mutableListOf<UserSpinnerItem>()
            items.add(UserSpinnerItem(0, "Sin asignar"))
            items.addAll(users.map { UserSpinnerItem(it.id, it.fullName) })

            val assigneeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
            )
            assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAssignee.adapter = assigneeAdapter

            // Intentar seleccionar el usuario actual si la tarea ya cargó
            updateSpinnersSelection()
        }
    }

    private fun updateSpinnersSelection() {
        val currentTask = task ?: return

        // Estado
        val estados = listOf("pendiente", "en progreso", "completado")
        val statusIndex = estados.indexOfFirst { it.equals(currentTask.estado, ignoreCase = true) }
        if (statusIndex >= 0) binding.spinnerStatus.setSelection(statusIndex)

        // Prioridad
        val prioridades = listOf("baja", "media", "alta")
        val priorityIndex = prioridades.indexOfFirst { it.equals(currentTask.prioridad, ignoreCase = true) }
        if (priorityIndex >= 0) binding.spinnerPriority.setSelection(priorityIndex)

        // Asignado (Solo si el adapter tiene datos)
        if (binding.spinnerAssignee.adapter != null && binding.spinnerAssignee.count > 0) {
            val adapter = binding.spinnerAssignee.adapter
            for (i in 0 until adapter.count) {
                val item = adapter.getItem(i) as UserSpinnerItem
                if (item.id == (currentTask.asignadoId ?: 0L)) {
                    binding.spinnerAssignee.setSelection(i)
                    break
                }
            }
        }
    }

    private fun setupDatePicker() {
        binding.etDueDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedDate?.let {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.time = format.parse(it)!!
                } catch (e: Exception) {}
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.etDueDate.setText(displayFormat.format(calendar.time))
                    val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    selectedDate = dbFormat.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                show()
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { updateTask() }
    }

    private fun updateTask() {
        val currentTask = task ?: return

        val titulo = binding.etTaskTitle.text.toString().trim()
        val descripcion = binding.etTaskDescription.text.toString().trim()

        val estados = listOf("pendiente", "en progreso", "completado")
        val prioridades = listOf("baja", "media", "alta")

        val estado = estados.getOrElse(binding.spinnerStatus.selectedItemPosition) { "pendiente" }
        val prioridad = prioridades.getOrElse(binding.spinnerPriority.selectedItemPosition) { "media" }

        val selectedUserItem = binding.spinnerAssignee.selectedItem as? UserSpinnerItem
        val asignadoId = if (selectedUserItem != null && selectedUserItem.id > 0) selectedUserItem.id else null

        if (titulo.isEmpty()) {
            Snackbar.make(binding.root, "El título es obligatorio", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Llamada al ViewModel
        viewModel.updateTask(
            taskId = currentTask.id,
            titulo = titulo,
            descripcion = descripcion.ifEmpty { null },
            prioridad = prioridad,
            fechaLimite = selectedDate,
            asignadoId = asignadoId
        )

        // Si cambió el estado, actualizar también (ya que updateTask no actualiza estado en tu PHP)
        if (!estado.equals(currentTask.estado, ignoreCase = true)) {
            viewModel.updateTaskState(currentTask.id, estado)
        }

        dismiss()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) { dateString }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}