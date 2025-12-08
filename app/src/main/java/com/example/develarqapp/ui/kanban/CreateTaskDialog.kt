package com.example.develarqapp.ui.kanban

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogCreateTaskBinding
import com.example.develarqapp.data.model.UserSpinnerItem
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class CreateTaskDialog : DialogFragment() {

    private var _binding: DialogCreateTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KanbanViewModel by activityViewModels()

    private var selectedDate: String? = null
    private var projectId: Long = 0

    companion object {
        private const val ARG_PROJECT_ID = "project_id"

        fun newInstance(projectId: Long): CreateTaskDialog {
            return CreateTaskDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        projectId = arguments?.getLong(ARG_PROJECT_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadUsers()
        setupPrioritySpinner()
        setupAssigneeSpinner()
        setupDatePicker()
        setupButtons()
    }

    private fun setupPrioritySpinner() {
        val prioridades = listOf("baja", "media", "alta")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            prioridades.map { it.uppercase() }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriority.adapter = adapter
        binding.spinnerPriority.setSelection(1) // media por defecto
    }

    private fun setupAssigneeSpinner() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            val items = mutableListOf<UserSpinnerItem>()
            items.add(UserSpinnerItem(0, "Sin asignar"))
            items.addAll(users.map { UserSpinnerItem(it.id, it.fullName) })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAssignee.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        binding.etDueDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)

                    // Formato para mostrar
                    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.etDueDate.setText(displayFormat.format(calendar.time))

                    // Formato para enviar al backend
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
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createTask()
        }
    }

    private fun createTask() {
        val titulo = binding.etTaskTitle.text.toString().trim()
        val descripcion = binding.etTaskDescription.text.toString().trim()
        val prioridadPos = binding.spinnerPriority.selectedItemPosition
        val asignadoPos = binding.spinnerAssignee.selectedItemPosition

        // Validaciones
        if (titulo.isEmpty()) {
            Snackbar.make(binding.root, "El título es obligatorio", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (projectId == 0L) {
            Snackbar.make(binding.root, "Error: Proyecto no seleccionado", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Obtener prioridad
        val prioridades = listOf("baja", "media", "alta")
        val prioridad = prioridades[prioridadPos]

        // Obtener asignado (null si es "Sin asignar")
        val asignadoId = if (asignadoPos > 0) {
            (binding.spinnerAssignee.selectedItem as UserSpinnerItem).id
        } else {
            null
        }

        // Crear tarea
        viewModel.createTask(
            projectId = projectId,
            titulo = titulo,
            descripcion = descripcion.ifEmpty { null },
            prioridad = prioridad,
            fechaLimite = selectedDate,
            asignadoId = asignadoId
        )

        // Observar resultado
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    it, Snackbar.LENGTH_SHORT).show()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}