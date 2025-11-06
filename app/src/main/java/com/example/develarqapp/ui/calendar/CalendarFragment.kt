package com.example.develarqapp.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import java.util.Calendar
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Meeting
import com.example.develarqapp.databinding.FragmentCalendarBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*


class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    private val apiFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val headerDateFormat = SimpleDateFormat("d 'de' MMM", Locale("es"))
    private val fullDateFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))

    private var currentViewMode = ViewMode.DAY
    private var currentDate = Calendar.getInstance()

    enum class ViewMode {
        DAY, THREE_DAY, WEEK
    }

    // Adapter para WeekView
    private val adapter = object : WeekView.SimpleAdapter<Meeting>() {
        override fun onCreateEntity(item: Meeting): WeekViewEntity {
            return try {
                val startTime = apiFormat.parse(item.fechaHora) ?: throw Exception("Fecha de inicio nula")
                val startCalendar = Calendar.getInstance().apply { time = startTime }

                val endCalendar = Calendar.getInstance()
                if (!item.fechaHoraFin.isNullOrEmpty()) {
                    val endTime = apiFormat.parse(item.fechaHoraFin)
                    if (endTime != null) {
                        endCalendar.time = endTime
                    } else {
                        endCalendar.time = startTime
                        endCalendar.add(Calendar.HOUR, 1)
                    }
                } else {
                    endCalendar.time = startTime
                    endCalendar.add(Calendar.HOUR, 1)
                }

                // Color basado en proyecto o estado
                val backgroundColor = getColorForMeeting(item)

                WeekViewEntity.Event.Builder(item)
                    .setId(item.id)
                    .setTitle(item.titulo)
                    .setSubtitle(item.proyectoNombre ?: "Sin proyecto")
                    .setStartTime(startCalendar)
                    .setEndTime(endCalendar)
                    .setStyle(
                        WeekViewEntity.Style.Builder()
                            .setBackgroundColor(backgroundColor)
                            .setTextColor(Color.BLACK)
                            .setCornerRadius(8)
                            .build()
                    )
                    .build()

            } catch (e: Exception) {
                e.printStackTrace()
                val now = Calendar.getInstance()
                val end = Calendar.getInstance().apply { add(Calendar.HOUR, 1) }
                WeekViewEntity.Event.Builder(item)
                    .setId(item.id)
                    .setTitle(item.titulo)
                    .setStartTime(now)
                    .setEndTime(end)
                    .build()
            }
        }

        override fun onEventClick(data: Meeting) {
            showMeetingOptionsDialog(data)
        }

        override fun onEventLongClick(data: Meeting) {
            showMeetingOptionsDialog(data)
        }
    }

    private fun getColorForMeeting(meeting: Meeting): Int {
        // Puedes personalizar colores por proyecto
        return ContextCompat.getColor(requireContext(), R.color.primary_green)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupTopBar()
        setupClickListeners()
        setupWeekView()
        setupObservers()
        loadInitialData()
        updateDateHeader()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    private fun loadInitialData() {
        val token = sessionManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("CalendarFragment", "Cargando datos con token: ${token.take(20)}...")
        viewModel.loadMeetings(token)
        viewModel.loadProjects(token)
        viewModel.loadUsers(token)
    }

    private fun setupWeekView() {
        binding.weekView.apply {
            adapter = this@CalendarFragment.adapter
            numberOfVisibleDays = 1 // Iniciar con vista de 1 día
            minHour = 8
            maxHour = 20
            headerTextColor = Color.WHITE
            headerBackgroundColor = "#1E293B".toColorInt()
            dayBackgroundColor = "#0F172A".toColorInt()
            todayBackgroundColor = "#1E293B".toColorInt()

            // Scroll al día actual
            goToToday()
        }
    }

    private fun setupClickListeners() {
        // FAB para crear nueva reunión
        binding.fabAddMeeting.setOnClickListener {
            val dialog = CreateMeetingDialogFragment()
            dialog.show(childFragmentManager, "CreateMeetingDialog")
        }

        // Navegación de fechas
        binding.btnPreviousDay.setOnClickListener {
            navigateDate(-1)
        }

        binding.btnToday.setOnClickListener {
            currentDate = Calendar.getInstance()
            binding.weekView.goToToday()
            updateDateHeader()
        }

        binding.btnNextDay.setOnClickListener {
            navigateDate(1)
        }

        // Botones de vista
        binding.btnViewDay.setOnClickListener {
            setViewMode(ViewMode.DAY)
        }

        binding.btnViewWeek.setOnClickListener {
            setViewMode(ViewMode.WEEK)
        }

        binding.btnViewMonth.setOnClickListener {
            // Vista de mes (puedes usar 7 días o implementar una vista personalizada)
            setViewMode(ViewMode.WEEK)
            Toast.makeText(context, "Vista de mes próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnViewAgenda.setOnClickListener {
            // Mostrar lista de reuniones
            showAgendaView()
        }

        // Botones de filtro de tiempo
        binding.btnFilterAll.setOnClickListener {
            viewModel.setTimeFilter(CalendarViewModel.MeetingFilter.ALL)
            updateFilterButtonUI(it)
        }

        binding.btnFilterProximas.setOnClickListener {
            viewModel.setTimeFilter(CalendarViewModel.MeetingFilter.UPCOMING)
            updateFilterButtonUI(it)
        }

        binding.btnFilterPasadas.setOnClickListener {
            viewModel.setTimeFilter(CalendarViewModel.MeetingFilter.PAST)
            updateFilterButtonUI(it)
        }
    }

    private fun navigateDate(days: Int) {
        when (currentViewMode) {
            ViewMode.DAY -> currentDate.add(Calendar.DAY_OF_MONTH, days)
            ViewMode.THREE_DAY -> currentDate.add(Calendar.DAY_OF_MONTH, days * 3)
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, days)
        }
        binding.weekView.goToDate(currentDate)
        updateDateHeader()
    }

    private fun setViewMode(mode: ViewMode) {
        currentViewMode = mode

        when (mode) {
            ViewMode.DAY -> {
                binding.weekView.numberOfVisibleDays = 1
                updateViewButtonUI(binding.btnViewDay)
            }
            ViewMode.THREE_DAY -> {
                binding.weekView.numberOfVisibleDays = 3
                updateViewButtonUI(binding.btnViewWeek)
            }
            ViewMode.WEEK -> {
                binding.weekView.numberOfVisibleDays = 7
                updateViewButtonUI(binding.btnViewWeek)
            }
        }

        updateDateHeader()
    }

    private fun updateDateHeader() {
        val calendar = currentDate.clone() as Calendar

        when (currentViewMode) {
            ViewMode.DAY -> {
                binding.tvCurrentDate.text = fullDateFormat.format(calendar.time).capitalize()
            }
            ViewMode.THREE_DAY -> {
                val startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 2)
                val endDate = calendar.time
                binding.tvCurrentDate.text = "${headerDateFormat.format(startDate)} - ${headerDateFormat.format(endDate)}"
            }
            ViewMode.WEEK -> {
                // Obtener inicio y fin de semana
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val endDate = calendar.time
                binding.tvCurrentDate.text = "${headerDateFormat.format(startDate)} - ${headerDateFormat.format(endDate)}"
            }
        }
    }

    private fun showAgendaView() {
        val dialog = AgendaDialogFragment()
        dialog.show(childFragmentManager, "AgendaDialog")
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }

        // Proyectos para spinner
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            android.util.Log.d("CalendarFragment", "Proyectos recibidos: ${projects.size}")
            if (projects.isEmpty()) {
                android.util.Log.w("CalendarFragment", "No hay proyectos disponibles")
                return@observe
            }

            val projectNames = mutableListOf("Todos los proyectos")
            projectNames.addAll(projects.map { it.nombre })

            val adapter = ArrayAdapter(
                requireContext(),
                R.drawable.spinner_item_white,
                projectNames
            )
            adapter.setDropDownViewResource(R.drawable.spinner_dropdown_item)
            binding.spinnerProjects.adapter = adapter

            binding.spinnerProjects.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.filterByProject(if (position == 0) null else projects[position - 1].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Usuarios para spinner
        viewModel.users.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                return@observe
            }

            val userNames = mutableListOf("Todos los participantes")
            userNames.addAll(users.map { it.fullName })

            val adapter = ArrayAdapter(
                requireContext(),
                R.drawable.spinner_item_white,
                userNames
            )
            adapter.setDropDownViewResource(R.drawable.spinner_dropdown_item)
            binding.spinnerParticipants.adapter = adapter

            binding.spinnerParticipants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.filterByUser(if (position == 0) null else users[position - 1].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Reuniones filtradas
        viewModel.filteredMeetings.observe(viewLifecycleOwner) { meetings ->
            updateWeekView(meetings)
        }
    }

    private fun updateWeekView(meetings: List<Meeting>) {
        adapter.submitList(meetings)
    }

    private fun showMeetingOptionsDialog(meeting: Meeting) {
        val options = arrayOf("Ver Detalles", "Eliminar")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(meeting.titulo)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMeetingDetails(meeting)
                    1 -> showDeleteConfirmation(meeting)
                }
            }
            .show()
    }

    private fun showMeetingDetails(meeting: Meeting) {
        val details = buildString {
            append("Proyecto: ${meeting.proyectoNombre ?: "N/A"}\n\n")
            append("Fecha: ${formatDateTime(meeting.fechaHora)}\n\n")
            if (!meeting.fechaHoraFin.isNullOrEmpty()) {
                append("Fecha fin: ${formatDateTime(meeting.fechaHoraFin)}\n\n")
            }
            if (!meeting.descripcion.isNullOrEmpty()) {
                append("Descripción: ${meeting.descripcion}\n\n")
            }
            if (!meeting.participantes.isNullOrEmpty()) {
                append("Participantes:\n")
                meeting.participantes.forEach {
                    append("• ${it.nombre}\n")
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(meeting.titulo)
            .setMessage(details)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showDeleteConfirmation(meeting: Meeting) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Reunión")
            .setMessage("¿Estás seguro de eliminar la reunión '${meeting.titulo}'?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.deleteMeeting(meeting.id, token)
                    viewModel.loadMeetings(token)
                } else {
                    Toast.makeText(context, "Error de sesión", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updateFilterButtonUI(clickedButton: View) {
        val inactiveColor = ContextCompat.getColorStateList(requireContext(), R.color.secondary_gray)
        val activeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)

        binding.btnFilterAll.backgroundTintList = inactiveColor
        binding.btnFilterProximas.backgroundTintList = inactiveColor
        binding.btnFilterPasadas.backgroundTintList = inactiveColor

        clickedButton.backgroundTintList = activeColor
    }

    private fun updateViewButtonUI(clickedButton: View) {
        val inactiveColor = ContextCompat.getColorStateList(requireContext(), R.color.secondary_gray)
        val activeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)

        binding.btnViewDay.backgroundTintList = inactiveColor
        binding.btnViewWeek.backgroundTintList = inactiveColor
        binding.btnViewMonth.backgroundTintList = inactiveColor
        binding.btnViewAgenda.backgroundTintList = inactiveColor

        clickedButton.backgroundTintList = activeColor
    }

    private fun formatDateTime(dateTimeStr: String): String {
        return try {
            val date = apiFormat.parse(dateTimeStr)
            val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            date?.let { displayFormat.format(it) } ?: dateTimeStr
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}