package com.example.develarqapp.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import java.util.Calendar
import java.util.Date
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Meeting
import com.example.develarqapp.databinding.FragmentCalendarBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    private val apiFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))
    private val shortDateFormat = SimpleDateFormat("d 'de' MMM", Locale("es"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var currentViewMode = ViewMode.DAY
    private var currentDate = Calendar.getInstance()
    private var currentMeetingList: List<Meeting> = emptyList()
    private lateinit var monthAdapter: MonthCalendarAdapter

    private val TAG = "CalendarFragment"

    enum class ViewMode {
        DAY, WEEK, MONTH, AGENDA
    }

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

                val builder = WeekViewEntity.Event.Builder(item)
                    .setId(item.id)
                    .setTitle(item.titulo)
                    .setStartTime(startCalendar)
                    .setEndTime(endCalendar)

                val subtitle = buildString {
                    append(item.proyectoNombre ?: "Sin proyecto")
                    append("\n")
                    append(timeFormat.format(startTime))
                    append(" - ")
                    append(timeFormat.format(endCalendar.time))
                }
                builder.setSubtitle(subtitle)

                builder.setStyle(
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
        setupWeekView()
        setupMonthView()
        setupClickListeners()
        setupObservers()
        loadInitialData()

        // ✅ Iniciar en el día actual
        currentDate = Calendar.getInstance()
        setViewMode(ViewMode.DAY)
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

        Log.d(TAG, "📄 Cargando datos iniciales...")
        viewModel.loadMeetings(token)
        viewModel.loadProjects(token)
        viewModel.loadUsers(token)
    }

    private fun setupWeekView() {
        binding.weekView.apply {
            adapter = this@CalendarFragment.adapter
            numberOfVisibleDays = 1
            minHour = 0
            maxHour = 24
            hourHeight = 120
            headerTextColor = Color.WHITE
            headerBackgroundColor = "#1E293B".toColorInt()
            dayBackgroundColor = "#0F172A".toColorInt()
            todayBackgroundColor = "#1E293B".toColorInt()
        }
    }

    private fun setupMonthView() {
        monthAdapter = MonthCalendarAdapter { date ->
            currentDate = Calendar.getInstance().apply { time = date }
            setViewMode(ViewMode.DAY)
        }
        binding.rvMonthCalendar.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = monthAdapter
        }
    }

    private fun updateMonthGrid() {
        val calendar = currentDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

        val days = mutableListOf<Date?>()

        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        for (i in 1..daysInMonth) {
            val dayCal = calendar.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, i)
            days.add(dayCal.time)
        }

        monthAdapter.setData(days, currentMeetingList)
    }

    private fun setupClickListeners() {
        binding.fabAddMeeting.setOnClickListener {
            val dialog = CreateMeetingDialogFragment()
            dialog.show(childFragmentManager, "CreateMeetingDialog")
        }

        // ✅ Botón "Hoy" - volver a la fecha actual
        binding.tvCurrentDate.setOnClickListener {
            currentDate = Calendar.getInstance()
            when (currentViewMode) {
                ViewMode.DAY, ViewMode.WEEK -> binding.weekView.goToToday()
                ViewMode.MONTH -> updateMonthGrid()
                else -> {}
            }
            updateDateHeader()
        }

        // ✅ Hacer el header clickeable para navegar
        binding.tvCurrentDate.setOnLongClickListener {
            showDateNavigationMenu()
            true
        }

        binding.btnViewDay.setOnClickListener {
            currentDate = Calendar.getInstance() // ✅ Reset a hoy
            setViewMode(ViewMode.DAY)
        }

        binding.btnViewWeek.setOnClickListener {
            currentDate = Calendar.getInstance() // ✅ Reset a esta semana
            setViewMode(ViewMode.WEEK)
        }

        binding.btnViewMonth.setOnClickListener {
            currentDate = Calendar.getInstance() // ✅ Reset a este mes
            setViewMode(ViewMode.MONTH)
        }

        binding.btnViewAgenda.setOnClickListener {
            setViewMode(ViewMode.AGENDA)
        }

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

    private fun showDateNavigationMenu() {
        val options = when (currentViewMode) {
            ViewMode.DAY -> arrayOf("Ir a hoy", "Día anterior", "Día siguiente", "Seleccionar fecha")
            ViewMode.WEEK -> arrayOf("Ir a esta semana", "Semana anterior", "Semana siguiente")
            ViewMode.MONTH -> arrayOf("Ir a este mes", "Mes anterior", "Mes siguiente")
            ViewMode.AGENDA -> return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Navegar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Ir a hoy/esta semana/este mes
                        currentDate = Calendar.getInstance()
                        when (currentViewMode) {
                            ViewMode.DAY, ViewMode.WEEK -> binding.weekView.goToDate(currentDate)
                            ViewMode.MONTH -> updateMonthGrid()
                            else -> {}
                        }
                        updateDateHeader()
                    }
                    1 -> navigateDate(-1) // Anterior
                    2 -> navigateDate(1)  // Siguiente
                    3 -> if (currentViewMode == ViewMode.DAY) showDatePicker()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                currentDate.set(year, month, dayOfMonth)
                binding.weekView.goToDate(currentDate)
                updateDateHeader()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun navigateDate(direction: Int) {
        when (currentViewMode) {
            ViewMode.DAY -> {
                currentDate.add(Calendar.DAY_OF_MONTH, direction)
                binding.weekView.goToDate(currentDate)
            }
            ViewMode.WEEK -> {
                currentDate.add(Calendar.WEEK_OF_YEAR, direction)
                binding.weekView.goToDate(currentDate)
            }
            ViewMode.MONTH -> {
                currentDate.add(Calendar.MONTH, direction)
                updateMonthGrid()
            }
            ViewMode.AGENDA -> {}
        }
        updateDateHeader()
    }

    private fun setViewMode(mode: ViewMode) {
        currentViewMode = mode

        val isMonthMode = mode == ViewMode.MONTH
        val isAgendaMode = mode == ViewMode.AGENDA

        binding.weekView.isVisible = !isMonthMode && !isAgendaMode
        binding.layoutMonthView.isVisible = isMonthMode

        when (mode) {
            ViewMode.DAY -> {
                binding.weekView.numberOfVisibleDays = 1
                binding.weekView.minHour = 0
                binding.weekView.maxHour = 24
                binding.weekView.hourHeight = 150
                binding.weekView.showTimeColumnSeparator = true
                binding.weekView.columnGap = 8
                binding.weekView.goToDate(currentDate)
                updateViewButtonUI(binding.btnViewDay)
                updateDateHeader()
            }
            ViewMode.WEEK -> {
                binding.weekView.numberOfVisibleDays = 7
                binding.weekView.minHour = 0
                binding.weekView.maxHour = 24
                binding.weekView.hourHeight = 200
                binding.weekView.showTimeColumnSeparator = true
                binding.weekView.columnGap = 4
                binding.weekView.goToDate(currentDate)
                updateViewButtonUI(binding.btnViewWeek)
                updateDateHeader()
            }
            ViewMode.MONTH -> {
                updateMonthGrid()
                updateViewButtonUI(binding.btnViewMonth)
                updateDateHeader()
            }
            ViewMode.AGENDA -> {
                updateViewButtonUI(binding.btnViewAgenda)
                binding.tvCurrentDate.text = "Agenda de Reuniones"
                showAgendaView()
            }
        }
    }

    private fun updateDateHeader() {
        val calendar = currentDate.clone() as Calendar

        when (currentViewMode) {
            ViewMode.DAY -> {
                binding.tvCurrentDate.text = dayFormat.format(calendar.time).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
            ViewMode.WEEK -> {
                // ✅ Calcular inicio y fin de la semana
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val endDate = calendar.time
                binding.tvCurrentDate.text = "${shortDateFormat.format(startDate)} - ${shortDateFormat.format(endDate)}"
            }
            ViewMode.MONTH -> {
                binding.tvCurrentDate.text = monthYearFormat.format(calendar.time).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
            ViewMode.AGENDA -> {
                binding.tvCurrentDate.text = "Agenda de Reuniones"
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
            Log.d(TAG, "⏳ Loading: $isLoading")
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "❌ Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.d(TAG, "✅ Success: $it")
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }

        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            Log.d(TAG, "📋 Proyectos recibidos: ${projects.size}")

            if (projects.isEmpty()) {
                Log.w(TAG, "⚠️ Lista de proyectos vacía")
                return@observe
            }

            val projectNames = mutableListOf("Todos los proyectos")
            projectNames.addAll(projects.map { it.nombre })

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_white,
                projectNames
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerProjects.adapter = adapter

            binding.spinnerProjects.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.filterByProject(if (position == 0) null else projects[position - 1].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "👥 Usuarios recibidos: ${users.size}")

            if (users.isEmpty()) {
                Log.w(TAG, "⚠️ Lista de usuarios vacía")
                return@observe
            }

            val userNames = mutableListOf("Todos los participantes")
            userNames.addAll(users.map { it.fullName })

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_white,
                userNames
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerParticipants.adapter = adapter

            binding.spinnerParticipants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.filterByUser(if (position == 0) null else users[position - 1].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        viewModel.filteredMeetings.observe(viewLifecycleOwner) { meetings ->
            Log.d(TAG, "📅 Reuniones filtradas: ${meetings.size}")
            updateWeekView(meetings)
        }
    }

    private fun updateWeekView(meetings: List<Meeting>) {
        currentMeetingList = meetings
        adapter.submitList(meetings)
        if (currentViewMode == ViewMode.MONTH) {
            updateMonthGrid()
        }
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

    // ==================== ADAPTER PARA VISTA DE MES ====================
    inner class MonthCalendarAdapter(private val onDayClick: (Date) -> Unit) :
        RecyclerView.Adapter<MonthCalendarAdapter.DayViewHolder>() {

        private var days: List<Date?> = emptyList()
        private var meetings: List<Meeting> = emptyList()
        private val calendar = Calendar.getInstance()

        fun setData(newDays: List<Date?>, newMeetings: List<Meeting>) {
            days = newDays
            meetings = newMeetings
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val date = days[position]
            if (date == null) {
                holder.tvDayNumber.text = ""
                holder.meetingsContainer.removeAllViews()
                holder.itemView.setOnClickListener(null)
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            } else {
                calendar.time = date
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                holder.tvDayNumber.text = dayOfMonth.toString()

                val dayMeetings = meetings.filter { meeting ->
                    val mDate = apiFormat.parse(meeting.fechaHora)
                    if (mDate != null) {
                        val mCal = Calendar.getInstance().apply { time = mDate }
                        mCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                                mCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                    } else false
                }

                holder.meetingsContainer.removeAllViews()
                // ✅ Limitar a 2 reuniones visibles para mejor presentación
                dayMeetings.take(2).forEach { meeting ->
                    val meetingView = LayoutInflater.from(holder.itemView.context)
                        .inflate(R.layout.item_meeting_compact, holder.meetingsContainer, false)

                    meetingView.findViewById<TextView>(R.id.tvMeetingTitle).text = meeting.titulo

                    val startTime = apiFormat.parse(meeting.fechaHora)
                    val endTime = if (!meeting.fechaHoraFin.isNullOrEmpty()) {
                        apiFormat.parse(meeting.fechaHoraFin)
                    } else null

                    val timeText = if (startTime != null) {
                        if (endTime != null) {
                            "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
                        } else {
                            timeFormat.format(startTime)
                        }
                    } else ""

                    meetingView.findViewById<TextView>(R.id.tvMeetingTime).text = timeText
                    meetingView.findViewById<TextView>(R.id.tvMeetingProject).text = meeting.proyectoNombre ?: ""

                    meetingView.setOnClickListener {
                        showEditMeetingDialog(meeting)
                    }

                    holder.meetingsContainer.addView(meetingView)
                }

                // ✅ Mostrar indicador si hay más reuniones
                if (dayMeetings.size > 2) {
                    val moreText = TextView(holder.itemView.context).apply {
                        text = "+${dayMeetings.size - 2} más"
                        textSize = 9f
                        setTextColor(ContextCompat.getColor(context, R.color.primary_green))
                        setPadding(4, 2, 4, 2)
                    }
                    holder.meetingsContainer.addView(moreText)
                }

                val today = Calendar.getInstance()
                if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    holder.tvDayNumber.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                } else {
                    holder.tvDayNumber.setTextColor(Color.WHITE)
                }

                holder.itemView.setOnClickListener { onDayClick(date) }
            }
        }

        override fun getItemCount() = days.size

        inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
            val meetingsContainer: ViewGroup = view.findViewById(R.id.meetingsContainer)
        }
    }
}