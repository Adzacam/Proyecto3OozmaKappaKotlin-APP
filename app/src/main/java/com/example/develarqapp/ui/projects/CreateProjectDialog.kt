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
import com.example.develarqapp.databinding.DialogCreateProjectBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class CreateProjectDialog : DialogFragment() {

    private var _binding: DialogCreateProjectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })

    private var clientesList: List<com.example.develarqapp.data.model.User> = emptyList()
    private var responsablesList: List<com.example.develarqapp.data.model.User> = emptyList()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCreateProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        viewModel.loadUsers()
    }

    private fun setupUI() {
        val estados = arrayOf("Activo", "En Progreso", "Finalizado")
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = estadoAdapter

        binding.etStartDate.setOnClickListener { showDatePicker(true) }

        binding.etEndDate.setOnClickListener { showDatePicker(false) }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener { createProject() }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            if (isStartDate) binding.etStartDate.setText(formattedDate) else binding.etEndDate.setText(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun createProject() {
        val nombre = binding.etProjectName.text.toString().trim()
        val descripcion = binding.etProjectDescription.text.toString().trim()
        val fechaInicio = binding.etStartDate.text.toString().trim()
        val fechaFin = binding.etEndDate.text.toString().trim().ifEmpty { null }
        val estado = binding.spinnerStatus.selectedItem.toString().lowercase()
        val clienteId = clientesList.getOrNull(binding.spinnerClient.selectedItemPosition)?.id
        val responsableId = responsablesList.getOrNull(binding.spinnerResponsable.selectedItemPosition)?.id

        if (nombre.isEmpty() || fechaInicio.isEmpty() || clienteId == null || responsableId == null) {
            Toast.makeText(requireContext(), "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.createProject(nombre, descripcion, estado, fechaInicio, fechaFin, clienteId, responsableId)
    }

    private fun observeViewModel() {

        viewModel.users.observe(viewLifecycleOwner) { users ->
            clientesList = users.filter { it.rol?.lowercase() == "cliente" }
            val clienteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, clientesList.map { it.fullName })
            clienteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClient.adapter = clienteAdapter

            responsablesList = users.filter { it.rol?.lowercase() in listOf("arquitecto", "ingeniero", "admin") }
            val responsableAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, responsablesList.map { it.fullName })
            responsableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerResponsable.adapter = responsableAdapter
        }


        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty() && isVisible) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}