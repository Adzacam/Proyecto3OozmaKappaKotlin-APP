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
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogCreateHitoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class CreateHitoDialog : DialogFragment() {

    private var _binding: DialogCreateHitoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })

    private var projectId: Long = 0
    private var projectName: String = ""
    private var encargadosList: List<com.example.develarqapp.data.model.User> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateHitoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener argumentos
        arguments?.let {
            projectId = it.getLong("project_id", 0)
            projectName = it.getString("project_name", "")
        }

        setupUI()
        observeViewModel()

        // Cargar usuarios para encargado
        viewModel.loadUsers()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .create()
    }

    private fun setupUI() {
        binding.tvProjectName.text = projectName

        // Spinner de estado
        val estados = arrayOf("Pendiente", "En Progreso", "Completado", "Bloqueado")
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = estadoAdapter

        // Date picker
        binding.etFechaHito.setOnClickListener { showDatePicker() }

        // Botones
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener { createHito() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                binding.etFechaHito.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun createHito() {
        val nombre = binding.etNombreHito.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val fechaHito = binding.etFechaHito.text.toString().trim()
        val estado = binding.spinnerEstado.selectedItem.toString()
        val encargadoId = encargadosList.getOrNull(binding.spinnerEncargado.selectedItemPosition)?.id

        // Validaciones
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre del hito es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaHito.isEmpty()) {
            Toast.makeText(requireContext(), "La fecha del hito es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear hito
        viewModel.createHito(
            proyectoId = projectId,
            nombre = nombre,
            descripcion = descripcion.ifEmpty { null },
            fechaHito = fechaHito,
            estado = estado,
            encargadoId = encargadoId
        )
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            // Filtrar usuarios que pueden ser encargados
            encargadosList = users.filter {
                it.rol?.lowercase() in listOf("arquitecto", "ingeniero", "admin")
            }

            val encargadoNames = listOf("Sin asignar") + encargadosList.map { it.fullName }
            val encargadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, encargadoNames)
            encargadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerEncargado.adapter = encargadoAdapter
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}