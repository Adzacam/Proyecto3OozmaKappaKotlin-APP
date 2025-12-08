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
import com.example.develarqapp.databinding.DialogEditHitoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class EditHitoDialog : DialogFragment() {

    private var _binding: DialogEditHitoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })

    private var hitoId: Long = 0
    private var encargadosList: List<com.example.develarqapp.data.model.User> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditHitoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadHitoData()
        observeViewModel()

        // Cargar usuarios
        viewModel.loadUsers()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .create()
    }

    private fun setupUI() {
        // Spinner de estado
        val estados = arrayOf("Pendiente", "En Progreso", "Completado", "Bloqueado")
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = estadoAdapter

        // Date picker
        binding.etFechaHito.setOnClickListener { showDatePicker() }

        // Botones
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { updateHito() }
    }

    private fun loadHitoData() {
        arguments?.let {
            hitoId = it.getLong("hito_id", 0)
            binding.etNombreHito.setText(it.getString("hito_nombre"))
            binding.etDescripcion.setText(it.getString("hito_descripcion"))
            binding.etFechaHito.setText(it.getString("hito_fecha"))

            // Seleccionar estado
            val estado = it.getString("hito_estado", "Pendiente")
            val estadoIndex = when (estado) {
                "EN_PROGRESO" -> 1
                "COMPLETADO" -> 2
                "BLOQUEADO" -> 3
                else -> 0
            }
            binding.spinnerEstado.setSelection(estadoIndex)
        }
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

    private fun updateHito() {
        val nombre = binding.etNombreHito.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val fechaHito = binding.etFechaHito.text.toString().trim()
        val estadoPosition = binding.spinnerEstado.selectedItemPosition
        val estado = when (estadoPosition) {
            1 -> "En Progreso"
            2 -> "Completado"
            3 -> "Bloqueado"
            else -> "Pendiente"
        }
        val encargadoId = encargadosList.getOrNull(binding.spinnerEncargado.selectedItemPosition - 1)?.id

        // Validaciones
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre del hito es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaHito.isEmpty()) {
            Toast.makeText(requireContext(), "La fecha del hito es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar hito
        viewModel.updateHito(
            id = hitoId,
            nombre = nombre,
            descripcion = descripcion.ifEmpty { null },
            fechaHito = fechaHito,
            estado = estado,
            encargadoId = encargadoId
        )
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            encargadosList = users.filter {
                it.rol?.lowercase() in listOf("arquitecto", "ingeniero", "admin")
            }

            val encargadoNames = listOf("Sin asignar") + encargadosList.map { it.fullName }
            val encargadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, encargadoNames)
            encargadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerEncargado.adapter = encargadoAdapter

            arguments?.let {
                val currentEncargadoId = it.getLong("hito_encargado_id", 0)
                if (currentEncargadoId > 0) {
                    val index = encargadosList.indexOfFirst { user -> user.id == currentEncargadoId }
                    if (index >= 0) {
                        binding.spinnerEncargado.setSelection(index + 1) // +1 por "Sin asignar"
                    }
                }
            }
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