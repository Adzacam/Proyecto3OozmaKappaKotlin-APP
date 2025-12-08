package com.example.develarqapp.ui.projects

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogEditProjectBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class EditProjectDialog : DialogFragment() {

    private var _binding: DialogEditProjectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })

    private var projectId: Long = 0
    private var projectNombre: String = ""
    private var projectDescripcion: String = ""
    private var projectEstado: String = ""
    private var projectFechaInicio: String = ""
    private var projectFechaFin: String? = null
    private var projectClienteId: Long = 0
    private var projectResponsableId: Long = 0

    private var clientesList: List<com.example.develarqapp.data.model.User> = emptyList()
    private var responsablesList: List<com.example.develarqapp.data.model.User> = emptyList()

    companion object {
        fun newInstance(
            projectId: Long,
            projectNombre: String,
            projectDescripcion: String,
            projectEstado: String,
            projectFechaInicio: String,
            projectFechaFin: String?,
            projectClienteId: Long,
            projectResponsableId: Long
        ): EditProjectDialog {
            val fragment = EditProjectDialog()
            val args = Bundle().apply {
                putLong("projectId", projectId)
                putString("projectNombre", projectNombre)
                putString("projectDescripcion", projectDescripcion)
                putString("projectEstado", projectEstado)
                putString("projectFechaInicio", projectFechaInicio)
                putString("projectFechaFin", projectFechaFin)
                putLong("projectClienteId", projectClienteId)
                putLong("projectResponsableId", projectResponsableId)
            }
            fragment.arguments = args
            return fragment
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener argumentos
        arguments?.let {
            projectId = it.getLong("projectId")
            projectNombre = it.getString("projectNombre") ?: ""
            projectDescripcion = it.getString("projectDescripcion") ?: ""
            projectEstado = it.getString("projectEstado") ?: ""
            projectFechaInicio = it.getString("projectFechaInicio") ?: ""
            projectFechaFin = it.getString("projectFechaFin")
            projectClienteId = it.getLong("projectClienteId")
            projectResponsableId = it.getLong("projectResponsableId")
        }

        setupUI()
        loadProjectData()
        observeViewModel()

        // Cargar usuarios
        viewModel.loadUsers()
    }

    private fun setupUI() {
        // Date pickers
        binding.etStartDate.setOnClickListener { showDatePicker() }

        // Botones
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { updateProject() }
    }

    private fun loadProjectData() {
        binding.etProjectName.setText(projectNombre)
        binding.etProjectDescription.setText(projectDescripcion)
        binding.etStartDate.setText(projectFechaInicio)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                binding.etStartDate.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateProject() {
        val nombre = binding.etProjectName.text.toString().trim()
        val descripcion = binding.etProjectDescription.text.toString().trim()
        val fechaInicio = binding.etStartDate.text.toString().trim()

        val clienteId = clientesList.getOrNull(binding.spinnerClient.selectedItemPosition)?.id
        val responsableId = responsablesList.getOrNull(binding.spinnerResponsable.selectedItemPosition)?.id

        // Validaciones
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre del proyecto es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaInicio.isEmpty()) {
            Toast.makeText(requireContext(), "La fecha de inicio es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        if (clienteId == null || responsableId == null) {
            Toast.makeText(requireContext(), "Debe seleccionar cliente y responsable", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar proyecto
        viewModel.updateProject(
            id = projectId,
            nombre = nombre,
            descripcion = descripcion,
            estado = projectEstado,
            fechaInicio = fechaInicio,
            fechaFin = projectFechaFin,
            clienteId = clienteId,
            responsableId = responsableId
        )
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            // Filtrar clientes
            clientesList = users.filter { it.rol?.lowercase() == "cliente" }
            val clienteNames = clientesList.map { it.fullName }
            val clienteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, clienteNames)
            clienteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClient.adapter = clienteAdapter

            val currentClienteIndex = clientesList.indexOfFirst { it.id == projectClienteId }
            if (currentClienteIndex >= 0) {
                binding.spinnerClient.setSelection(currentClienteIndex)
            }

            responsablesList = users.filter {
                it.rol?.lowercase() in listOf("arquitecto", "ingeniero", "admin")
            }
            val responsableNames = responsablesList.map { it.fullName }
            val responsableAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, responsableNames)
            responsableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerResponsable.adapter = responsableAdapter

            val currentResponsableIndex = responsablesList.indexOfFirst { it.id == projectResponsableId }
            if (currentResponsableIndex >= 0) {
                binding.spinnerResponsable.setSelection(currentResponsableIndex)
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}