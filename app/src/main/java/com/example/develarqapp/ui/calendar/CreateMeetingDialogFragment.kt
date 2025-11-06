package com.example.develarqapp.ui.calendar

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.DialogCreateMeetingBinding
import com.example.develarqapp.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class CreateMeetingDialogFragment : DialogFragment() {

    private var _binding: DialogCreateMeetingBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels para compartir el ViewModel con CalendarFragment
    private val viewModel: CalendarViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    // Listas para spinners
    private var projectsList: List<Project> = emptyList()
    private var usersList: List<User> = emptyList()

    // Datos seleccionados
    private var selectedProjectId: Long? = null
    private var selectedStartCalendar = Calendar.getInstance()
    private var selectedEndCalendar = Calendar.getInstance()
    private var selectedParticipantIds = mutableListOf<Long>()
    private var selectedParticipantNames = mutableListOf<String>()

    // Formato para API
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    // Formato para mostrar al usuario
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCreateMeetingBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnCreate.setOnClickListener {
            validateAndCreateMeeting()
        }

        binding.etStartTime.setOnClickListener {
            showDateTimePicker(true)
        }
        binding.etEndTime.setOnClickListener {
            showDateTimePicker(false)
        }

        binding.etParticipants.setOnClickListener {
            showParticipantSelector()
        }
    }

    private fun setupObservers() {
        // Observar proyectos para el spinner
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            projectsList = projects
            val projectNames = projects.map { it.nombre }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, projectNames)
            binding.actvProject.setAdapter(adapter)

            binding.actvProject.setOnItemClickListener { parent, _, position, _ ->
                selectedProjectId = projectsList[position].id
            }
        }

        // Observar usuarios para el multi-selector
        viewModel.users.observe(viewLifecycleOwner) { users ->
            usersList = users
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarDialog.isVisible = isLoading
            binding.llButtons.isVisible = !isLoading
        }

        // Observar errores
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observar éxito de la operación
        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                dismiss()
                viewModel.resetOperationSuccess() // Resetear el flag
            }
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val calendar = if (isStart) selectedStartCalendar else selectedEndCalendar

        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                val selectedDate = displayDateFormat.format(calendar.time)

                if (isStart) {
                    binding.etStartTime.setText(selectedDate)
                } else {
                    binding.etEndTime.setText(selectedDate)
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showParticipantSelector() {
        val userNames = usersList.map { it.fullName }.toTypedArray()
        val checkedItems = usersList.map { selectedParticipantIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Participantes")
            .setMultiChoiceItems(userNames, checkedItems) { _, which, isChecked ->
                val userId = usersList[which].id
                val userName = usersList[which].fullName
                if (isChecked) {
                    selectedParticipantIds.add(userId)
                    selectedParticipantNames.add(userName)
                } else {
                    selectedParticipantIds.remove(userId)
                    selectedParticipantNames.remove(userName)
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                binding.etParticipants.setText("${selectedParticipantIds.size} seleccionado(s)")
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validateAndCreateMeeting() {
        val token = sessionManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.etTitle.text.toString()
        val description = binding.etDescription.text.toString()
        val startTime = apiDateFormat.format(selectedStartCalendar.time)

        val endTime = if (binding.etEndTime.text.toString().isNotEmpty()) {
            apiDateFormat.format(selectedEndCalendar.time)
        } else {
            null
        }

        viewModel.createMeeting(
            token = token,
            projectId = selectedProjectId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            participantIds = selectedParticipantIds
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}