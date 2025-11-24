package com.example.develarqapp.ui.calendar

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.data.model.Meeting
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.DialogEditMeetingBinding
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*
import com.example.develarqapp.R

class EditMeetingDialogFragment : DialogFragment() {

    private var _binding: DialogEditMeetingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    private lateinit var meeting: Meeting
    private var projectsList: List<Project> = emptyList()
    private var usersList: List<User> = emptyList()

    private var selectedProjectId: Long? = null
    private var selectedStartCalendar = Calendar.getInstance()
    private var selectedEndCalendar = Calendar.getInstance()
    private var selectedParticipantIds = mutableListOf<Long>()
    private var selectedParticipantNames = mutableListOf<String>()

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val TAG = "EditMeetingDialog"

    companion object {
        private const val ARG_MEETING = "meeting"

        fun newInstance(meeting: Meeting): EditMeetingDialogFragment {
            return EditMeetingDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MEETING, meeting)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        meeting = arguments?.getSerializable(ARG_MEETING) as Meeting
        Log.d(TAG, "📝 Editando reunión: ${meeting.titulo}")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditMeetingBinding.inflate(inflater, container, false)
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

        // ✅ Cargar datos de la reunión primero
        loadMeetingData()

        setupClickListeners()
        setupObservers()

        val token = sessionManager.getToken()
        if (token != null) {
            // ✅ Forzar recarga si las listas están vacías
            if (viewModel.projects.value.isNullOrEmpty()) {
                Log.d(TAG, "🔄 Recargando proyectos...")
                viewModel.loadProjects(token)
            } else {
                // Si ya hay proyectos, configurar el dropdown inmediatamente
                setupProjectDropdown(viewModel.projects.value ?: emptyList())
            }

            if (viewModel.users.value.isNullOrEmpty()) {
                Log.d(TAG, "🔄 Recargando usuarios...")
                viewModel.loadUsers(token)
            } else {
                usersList = viewModel.users.value ?: emptyList()
            }
        }
    }

    private fun loadMeetingData() {
        Log.d(TAG, "📋 Cargando datos de la reunión...")

        // ✅ Cargar título
        binding.etTitle.setText(meeting.titulo)
        Log.d(TAG, "  Título: ${meeting.titulo}")

        // ✅ Cargar descripción
        binding.etDescription.setText(meeting.descripcion ?: "")
        Log.d(TAG, "  Descripción: ${meeting.descripcion}")

        // ✅ Guardar proyecto seleccionado
        selectedProjectId = meeting.proyectoId
        Log.d(TAG, "  Proyecto ID: $selectedProjectId")

        // ✅ Parsear y mostrar fechas
        try {
            apiDateFormat.parse(meeting.fechaHora)?.let { startDate ->
                selectedStartCalendar.time = startDate
                binding.etStartTime.setText(displayDateFormat.format(startDate))
                Log.d(TAG, "  Fecha inicio: ${displayDateFormat.format(startDate)}")
            }

            meeting.fechaHoraFin?.let { fechaFin ->
                apiDateFormat.parse(fechaFin)?.let { endDate ->
                    selectedEndCalendar.time = endDate
                    binding.etEndTime.setText(displayDateFormat.format(endDate))
                    Log.d(TAG, "  Fecha fin: ${displayDateFormat.format(endDate)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parseando fechas", e)
        }

        // ✅ Cargar participantes
        meeting.participantes?.let { participantes ->
            selectedParticipantIds = participantes.map { it.userId }.toMutableList()
            selectedParticipantNames = participantes.map { it.nombre }.toMutableList()
            binding.etParticipants.setText("${selectedParticipantIds.size} seleccionado(s)")
            Log.d(TAG, "  Participantes: ${selectedParticipantIds.size}")
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            validateAndUpdateMeeting()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
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
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            Log.d(TAG, "📋 Proyectos recibidos: ${projects.size}")
            setupProjectDropdown(projects)
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "👥 Usuarios recibidos: ${users.size}")
            usersList = users
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarDialog.isVisible = isLoading
            binding.llButtons.isVisible = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                dismiss()
                viewModel.resetOperationSuccess()
            }
        }
    }

    private fun setupProjectDropdown(projects: List<Project>) {
        if (projects.isEmpty()) {
            Log.w(TAG, "⚠️ Lista de proyectos vacía")
            return
        }

        projectsList = projects

        val projectNames = projects.map { it.nombre }
        Log.d(TAG, "🎨 Configurando dropdown con proyectos: $projectNames")

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white, projectNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.actvProject.setAdapter(adapter)
        binding.actvProject.threshold = 1000

        // ✅ Preseleccionar el proyecto actual
        val currentProject = projects.find { it.id == meeting.proyectoId }
        currentProject?.let {
            Log.d(TAG, "✅ Preseleccionando proyecto: ${it.nombre}")
            binding.actvProject.setText(it.nombre, false)
            selectedProjectId = it.id
        }

        // ✅ Configurar listeners
        binding.actvProject.setOnClickListener {
            binding.actvProject.showDropDown()
        }

        binding.actvProject.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && projectsList.isNotEmpty()) {
                binding.actvProject.showDropDown()
            }
        }

        binding.actvProject.setOnItemClickListener { _, _, position, _ ->
            selectedProjectId = projectsList[position].id
            Log.d(TAG, "✅ Proyecto seleccionado: ${projectsList[position].nombre}")
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val calendar = if (isStart) selectedStartCalendar else selectedEndCalendar
        val today = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (selectedDate.before(todayStart)) {
                Toast.makeText(context, "No puedes seleccionar fechas pasadas", Toast.LENGTH_SHORT).show()
                return@DatePickerDialog
            }

            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0)

                val now = Calendar.getInstance()

                if (isSameDay(calendar, now) && calendar.before(now)) {
                    Toast.makeText(context, "No puedes seleccionar horas pasadas", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                if (!isStart) {
                    if (!isSameDay(selectedStartCalendar, calendar)) {
                        Toast.makeText(context, "La reunión debe ser en el mismo día", Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }

                    if (calendar.timeInMillis <= selectedStartCalendar.timeInMillis) {
                        Toast.makeText(context, "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }
                }

                val selectedDateTime = displayDateFormat.format(calendar.time)
                if (isStart) {
                    binding.etStartTime.setText(selectedDateTime)
                    binding.tilStartTime.error = null

                    if (selectedEndCalendar.timeInMillis <= selectedStartCalendar.timeInMillis ||
                        !isSameDay(selectedStartCalendar, selectedEndCalendar)) {
                        selectedEndCalendar = selectedStartCalendar.clone() as Calendar
                        selectedEndCalendar.add(Calendar.HOUR, 1)
                        binding.etEndTime.setText(displayDateFormat.format(selectedEndCalendar.time))
                    }
                } else {
                    binding.etEndTime.setText(selectedDateTime)
                    binding.tilEndTime.error = null
                }

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = today.timeInMillis
        }.show()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showParticipantSelector() {
        if (usersList.isEmpty()) {
            Toast.makeText(context, "No hay usuarios disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val userNames = usersList.map { it.fullName }.toTypedArray()
        val checkedItems = usersList.map { selectedParticipantIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Participantes")
            .setMultiChoiceItems(userNames, checkedItems) { _, which, isChecked ->
                val userId = usersList[which].id
                val userName = usersList[which].fullName
                if (isChecked) {
                    if (!selectedParticipantIds.contains(userId)) {
                        selectedParticipantIds.add(userId)
                        selectedParticipantNames.add(userName)
                    }
                } else {
                    selectedParticipantIds.remove(userId)
                    selectedParticipantNames.remove(userName)
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                binding.etParticipants.setText("${selectedParticipantIds.size} seleccionado(s)")
                binding.tilParticipants.error = null
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validateAndUpdateMeeting() {
        val token = sessionManager.getToken() ?: return

        binding.tilTitle.error = null
        binding.tilProject.error = null
        binding.tilStartTime.error = null
        binding.tilParticipants.error = null

        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        var hasError = false

        if (title.isEmpty()) {
            binding.tilTitle.error = "El título es requerido"
            hasError = true
        }

        if (selectedProjectId == null) {
            binding.tilProject.error = "Debes seleccionar un proyecto"
            hasError = true
        }

        if (selectedParticipantIds.isEmpty()) {
            binding.tilParticipants.error = "Debes seleccionar al menos un participante"
            hasError = true
        }

        if (binding.etEndTime.text.toString().isNotEmpty()) {
            if (!isSameDay(selectedStartCalendar, selectedEndCalendar)) {
                Toast.makeText(context, "La reunión debe ser en el mismo día", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            if (selectedEndCalendar.timeInMillis <= selectedStartCalendar.timeInMillis) {
                Toast.makeText(context, "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                hasError = true
            }
        }

        if (hasError) return

        val startTime = apiDateFormat.format(selectedStartCalendar.time)
        val endTime = if (binding.etEndTime.text.toString().isNotEmpty()) {
            apiDateFormat.format(selectedEndCalendar.time)
        } else {
            null
        }

        viewModel.updateMeeting(
            meetingId = meeting.id,
            token = token,
            projectId = selectedProjectId!!,
            title = title,
            description = description.ifEmpty { null },
            startTime = startTime,
            endTime = endTime,
            participantIds = selectedParticipantIds
        )
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Reunión")
            .setMessage("¿Estás seguro de eliminar la reunión '${meeting.titulo}'?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.deleteMeeting(meeting.id, token)
                    dismiss()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}