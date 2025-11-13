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

    private var currentViewMode = ViewMode.MONTH
    private var currentDate = Calendar.getInstance()

    enum class ViewMode {
        DAY, WEEK, MONTH, AGENDA
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

                val backgroundColor = ContextCompat.getColor(requireContext(), R.color.primary_green)

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
            showEditMeetingDialog(data)
        }

        override fun onEventLongClick(data: Meeting) {
            showEditMeetingDialog(data)
        }
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
        setViewMode(ViewMode.MONTH) // Iniciar en vista de mes
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

        viewModel.loadMeetings(token)
        viewModel.loadProjects(token)
        viewModel.loadUsers(token)
    }

    private fun setupWeekView() {
        binding.weekView.apply {
            adapter = this@CalendarFragment.adapter
            numberOfVisibleDays = 7
            minHour = 8
            maxHour = 20
            headerTextColor = Color.WHITE
            headerBackgroundColor = "#1E293B".toColorInt()
            dayBackgroundColor = "#0F172A".toColorInt()
            todayBackgroundColor = "#1E293B".toColorInt()
            goToToday()
        }
    }

    private fun setupClickListeners() {
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
            setViewMode(ViewMode.MONTH)
        }

        binding.btnViewAgenda.setOnClickListener {
            setViewMode(ViewMode.AGENDA)
        }

        // Filtros de tiempo
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
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, days)
            ViewMode.MONTH -> currentDate.add(Calendar.MONTH, days)
            ViewMode.AGENDA -> currentDate.add(Calendar.DAY_OF_MONTH, days)
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
            ViewMode.WEEK -> {
                binding.weekView.numberOfVisibleDays = 7
                updateViewButtonUI(binding.btnViewWeek)
            }
            ViewMode.MONTH -> {
                binding.weekView.numberOfVisibleDays = 7
                updateViewButtonUI(binding.btnViewMonth)
            }
            ViewMode.AGENDA -> {
                showAgendaView()
                updateViewButtonUI(binding.btnViewAgenda)
                return
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
            ViewMode.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val endDate = calendar.time
                binding.tvCurrentDate.text = "${headerDateFormat.format(startDate)} - ${headerDateFormat.format(endDate)}"
            }
            ViewMode.MONTH -> {
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es"))
                binding.tvCurrentDate.text = monthFormat.format(calendar.time).capitalize()
            }
            ViewMode.AGENDA -> {
                binding.tvCurrentDate.text = "Agenda"
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
            if (projects.isEmpty()) return@observe

            val projectNames = mutableListOf("Todos los proyectos")
            projectNames.addAll(projects.map { it.nombre })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                projectNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
            if (users.isEmpty()) return@observe

            val userNames = mutableListOf("Todos los participantes")
            userNames.addAll(users.map { it.fullName })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                userNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

    private fun showEditMeetingDialog(meeting: Meeting) {
        val dialog = EditMeetingDialogFragment.newInstance(meeting)
        dialog.show(childFragmentManager, "EditMeetingDialog")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}