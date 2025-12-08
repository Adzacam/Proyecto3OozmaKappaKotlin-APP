package com.example.develarqapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.databinding.FragmentDashboardBinding
import com.example.develarqapp.utils.DeviceInfoUtil
import com.example.develarqapp.utils.RoleColorsHelper
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    private val apiService by lazy { ApiConfig.getApiService() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("DashboardFragment", "🚀 Inicializando Dashboard")

        setupTopBar()
        applyRoleColors()

        viewModel.loadUserData(sessionManager)
        viewModel.loadDashboardStats(sessionManager)

        observeViewModel()
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    /**
     * Aplica los colores según el rol del usuario
     */
    private fun applyRoleColors() {
        val userRole = sessionManager.getUserRol()
        val accentColor = RoleColorsHelper.getAccentColor(requireContext(), userRole)
        binding.tvRole.setTextColor(accentColor)
    }

    private fun observeViewModel() {
        // Observar nombre del usuario
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            val fullName = "$name ${sessionManager.getUserApellido()}"
            binding.tvWelcome.text = "Bienvenido, $fullName"
        }

        // Observar rol del usuario
        viewModel.userRole.observe(viewLifecycleOwner) { role ->
            val displayRole = RoleColorsHelper.getRoleDisplayName(role)
            binding.tvRole.text = displayRole
            updateNavigationMenu(role)
        }

        // Observar estadísticas del dashboard
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            Log.d("DashboardFragment", "📊 Actualizando UI con estadísticas")
            updateDashboardUI(stats)
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        // Observar errores
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Actualiza toda la UI del dashboard según las estadísticas recibidas
     */
    private fun updateDashboardUI(stats: DashboardStats?) {
        stats ?: return

        val userRole = sessionManager.getUserRol().lowercase()

        when (userRole) {
            "admin" -> setupAdminDashboard(stats)
            "ingeniero", "arquitecto" -> setupEngineerArchitectDashboard(stats)
            "cliente" -> setupClienteDashboard(stats)
        }
    }

    /**
     * Configurar dashboard para ADMIN
     */
    private fun setupAdminDashboard(stats: DashboardStats) {
        Log.d("DashboardFragment", "👑 Configurando dashboard de ADMIN")

        // Mostrar sección de Mis Asignaciones
        binding.llMisAsignaciones.isVisible = true
        setupMisAsignaciones(stats)

        // Mostrar Vista General del Sistema
        binding.llVistaGeneral.isVisible = true
        setupVistaGeneralSistema(stats)
    }

    /**
     * Configurar dashboard para INGENIERO/ARQUITECTO
     */
    private fun setupEngineerArchitectDashboard(stats: DashboardStats) {
        Log.d("DashboardFragment", "⚙️ Configurando dashboard de INGENIERO/ARQUITECTO")

        // Mostrar solo Mis Asignaciones
        binding.llMisAsignaciones.isVisible = true
        setupMisAsignaciones(stats)

        // Mostrar proyectos asignados
        setupProyectosAsignados(stats)
    }

    /**
     * Configurar dashboard para CLIENTE
     */
    private fun setupClienteDashboard(stats: DashboardStats) {
        Log.d("DashboardFragment", "👤 Configurando dashboard de CLIENTE")

        // Mostrar proyectos asignados
        setupProyectosAsignados(stats)

        // Mostrar estadísticas básicas
        binding.cardProximasReuniones.isVisible = true
        setupProximasReuniones(stats)
    }

    /**
     * Configurar sección "Mis Asignaciones"
     */
    private fun setupMisAsignaciones(stats: DashboardStats) {
        val userRole = sessionManager.getUserRol()
        val accentColor = RoleColorsHelper.getAccentColor(requireContext(), userRole)

        // Card: Mis Proyectos
        binding.cardMisProyectos.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.misProyectos ?: 0}"
            setTextColor(accentColor)
        }
        binding.cardMisProyectos.root.findViewById<TextView>(R.id.tvStatLabel).text = "Mis Proyectos"

        // Card: Mis Tareas
        binding.cardMisTareas.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.misTareas ?: 0}"
            setTextColor(Color.parseColor("#F59E0B"))
        }
        binding.cardMisTareas.root.findViewById<TextView>(R.id.tvStatLabel).text = "Mis Tareas"

        // Card: Mis Reuniones
        binding.cardMisReuniones.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.misReuniones ?: 0}"
            setTextColor(Color.parseColor("#8B5CF6"))
        }
        binding.cardMisReuniones.root.findViewById<TextView>(R.id.tvStatLabel).text = "Mis Reuniones"

        // Card: Documentos (si aplica)
        val totalDocs = stats.totalDocumentos ?: 0
        binding.cardMisDocumentos.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "$totalDocs"
            setTextColor(Color.parseColor("#EC4899"))
        }
        binding.cardMisDocumentos.root.findViewById<TextView>(R.id.tvStatLabel).text = "Documentos"

        // Gráfica de Mis Tareas por Estado
        stats.misTareasPorEstado?.let { tareasEstado ->
            if (tareasEstado.isNotEmpty()) {
                binding.cardMisTareasChart.isVisible = true
                setupTareasEstadoChart(binding.llMisTareasEstado, tareasEstado)
            }
        }

        // Tareas Pendientes
        stats.tareasPendientes?.let { tareas ->
            if (tareas.isNotEmpty()) {
                binding.cardTareasPendientes.isVisible = true
                binding.llTareasPendientes.removeAllViews()
                binding.tvNoTareas.isVisible = false

                tareas.forEach { tarea ->
                    val taskView = createTaskMiniView(tarea)
                    binding.llTareasPendientes.addView(taskView)
                }
            } else {
                binding.cardTareasPendientes.isVisible = true
                binding.tvNoTareas.isVisible = true
            }
        }

        // Próximas Reuniones
        setupProximasReuniones(stats)
    }

    /**
     * Configurar Vista General del Sistema (ADMIN)
     */
    private fun setupVistaGeneralSistema(stats: DashboardStats) {
        // Total Proyectos
        binding.cardTotalProyectos.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.totalProyectos ?: 0}"
            setTextColor(Color.parseColor("#60A5FA"))
        }
        binding.cardTotalProyectos.root.findViewById<TextView>(R.id.tvStatLabel).text = "Total Proyectos"

        // Total Tareas
        binding.cardTotalTareas.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.totalTareas ?: 0}"
            setTextColor(Color.parseColor("#F59E0B"))
        }
        binding.cardTotalTareas.root.findViewById<TextView>(R.id.tvStatLabel).text = "Total Tareas"

        // Reuniones Próximas
        binding.cardReunionesProximas.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.reunionesProximas ?: 0}"
            setTextColor(Color.parseColor("#8B5CF6"))
        }
        binding.cardReunionesProximas.root.findViewById<TextView>(R.id.tvStatLabel).text = "Reuniones (7 días)"

        // Total Documentos
        binding.cardTotalDocumentos.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.totalDocumentos ?: 0}"
            setTextColor(Color.parseColor("#EC4899"))
        }
        binding.cardTotalDocumentos.root.findViewById<TextView>(R.id.tvStatLabel).text = "Documentos"

        // Usuarios Activos
        binding.cardUsuariosActivos.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.usuariosActivos ?: 0}"
            setTextColor(Color.parseColor("#10B981"))
        }
        binding.cardUsuariosActivos.root.findViewById<TextView>(R.id.tvStatLabel).text = "Usuarios Activos"

        // Notificaciones
        binding.cardNotificaciones.root.findViewById<TextView>(R.id.tvStatNumber).apply {
            text = "${stats.notificacionesNoLeidas ?: 0}"
            setTextColor(Color.parseColor("#EF4444"))
        }
        binding.cardNotificaciones.root.findViewById<TextView>(R.id.tvStatLabel).text = "Notificaciones"

        // Gráfica de Proyectos por Estado
        stats.proyectosPorEstado?.let { proyectosEstado ->
            if (proyectosEstado.isNotEmpty()) {
                binding.cardProyectosChart.isVisible = true
                setupProyectosEstadoChart(binding.llProyectosEstado, proyectosEstado)
            }
        }

        // Gráfica de Tareas por Estado
        stats.tareasPorEstado?.let { tareasEstado ->
            if (tareasEstado.isNotEmpty()) {
                binding.cardTareasChart.isVisible = true
                setupTareasEstadoChart(binding.llTareasEstado, tareasEstado)
            }
        }

        // Actividad Reciente
        stats.actividadReciente?.let { actividades ->
            if (actividades.isNotEmpty()) {
                binding.cardActividadReciente.isVisible = true
                setupActividadReciente(actividades)
            }
        }
    }

    /**
     * Configurar Proyectos Asignados
     */
    private fun setupProyectosAsignados(stats: DashboardStats) {
        stats.proyectosAsignados?.let { proyectos ->
            if (proyectos.isNotEmpty()) {
                binding.cardProyectosAsignados.isVisible = true
                binding.llProyectosAsignados.removeAllViews()
                binding.tvNoProyectos.isVisible = false

                proyectos.forEach { proyecto ->
                    val proyectoView = createProyectoMiniView(proyecto)
                    binding.llProyectosAsignados.addView(proyectoView)
                }
            } else {
                binding.cardProyectosAsignados.isVisible = true
                binding.tvNoProyectos.isVisible = true
            }
        }
    }

    /**
     * Configurar Próximas Reuniones
     */
    private fun setupProximasReuniones(stats: DashboardStats) {
        stats.proximasReuniones?.let { reuniones ->
            if (reuniones.isNotEmpty()) {
                binding.cardProximasReuniones.isVisible = true
                binding.llProximasReuniones.removeAllViews()
                binding.tvNoReuniones.isVisible = false

                reuniones.forEach { reunion ->
                    val reunionView = createReunionMiniView(reunion)
                    binding.llProximasReuniones.addView(reunionView)
                }
            } else {
                binding.cardProximasReuniones.isVisible = true
                binding.tvNoReuniones.isVisible = true
            }
        }
    }

    /**
     * Crear gráfica de barras para Proyectos por Estado
     */
    private fun setupProyectosEstadoChart(container: LinearLayout, estados: List<EstadoCount>) {
        container.removeAllViews()
        val total = estados.sumOf { it.cantidad }

        estados.forEach { estado ->
            val porcentaje = viewModel.calcularPorcentaje(estado.cantidad, total)
            val barView = createChartBar(
                label = estado.estado.capitalize(Locale.ROOT),
                cantidad = estado.cantidad,
                porcentaje = porcentaje,
                color = viewModel.getProyectoEstadoColor(estado.estado)
            )
            container.addView(barView)
        }
    }

    /**
     * Crear gráfica de barras para Tareas por Estado
     */
    private fun setupTareasEstadoChart(container: LinearLayout, estados: List<EstadoCount>) {
        container.removeAllViews()
        val total = estados.sumOf { it.cantidad }

        estados.forEach { estado ->
            val porcentaje = viewModel.calcularPorcentaje(estado.cantidad, total)
            val barView = createChartBar(
                label = estado.estado.replace("_", " ").capitalize(Locale.ROOT),
                cantidad = estado.cantidad,
                porcentaje = porcentaje,
                color = viewModel.getTareaEstadoColor(estado.estado)
            )
            container.addView(barView)
        }
    }

    /**
     * Crear barra de gráfica
     */
    private fun createChartBar(label: String, cantidad: Int, porcentaje: Float, color: String): View {
        val barLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // Label y cantidad
        val labelText = TextView(requireContext()).apply {
            text = "$label ($cantidad)"
            textSize = 14f
            setTextColor(Color.parseColor("#CBD5E1"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.3f
            )
        }

        // Container de la barra
        val barContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.5f
            )
            orientation = LinearLayout.VERTICAL
        }

        // Barra de progreso
        val progressBar = ProgressBar(
            requireContext(),
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = porcentaje.toInt()
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }

        // Porcentaje
        val porcentajeText = TextView(requireContext()).apply {
            text = "${porcentaje.toInt()}%"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.2f
            )
            textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        }

        barContainer.addView(progressBar)
        barLayout.addView(labelText)
        barLayout.addView(barContainer)
        barLayout.addView(porcentajeText)

        return barLayout
    }

    /**
     * Crear vista mini de tarea
     */
    private fun createTaskMiniView(tarea: TareaResumen): View {
        val view = layoutInflater.inflate(R.layout.item_task_mini, null)

        view.findViewById<TextView>(R.id.tvTaskTitle).text = tarea.titulo
        view.findViewById<TextView>(R.id.tvTaskProject).text = tarea.proyectoNombre ?: "Sin proyecto"

        val priorityView = view.findViewById<TextView>(R.id.tvTaskPriority)
        priorityView.text = tarea.prioridad.uppercase()
        priorityView.setTextColor(Color.parseColor(viewModel.getTareaPrioridadColor(tarea.prioridad)))

        return view
    }

    /**
     * Crear vista mini de reunión
     */
    private fun createReunionMiniView(reunion: ReunionResumen): View {
        val view = layoutInflater.inflate(R.layout.item_meeting_mini, null)

        view.findViewById<TextView>(R.id.tvMeetingTitle).text = reunion.titulo

        val dateTime = formatDateTime(reunion.fechaHora)
        view.findViewById<TextView>(R.id.tvMeetingDateTime).text = dateTime

        return view
    }

    /**
     * Crear vista mini de proyecto
     */
    private fun createProyectoMiniView(proyecto: ProyectoResumen): View {
        val view = layoutInflater.inflate(R.layout.item_task_mini, null)

        view.findViewById<TextView>(R.id.tvTaskTitle).text = proyecto.nombre
        view.findViewById<TextView>(R.id.tvTaskProject).text = "Progreso: ${proyecto.progreso}%"

        val estadoView = view.findViewById<TextView>(R.id.tvTaskPriority)
        estadoView.text = proyecto.estado.uppercase()
        estadoView.setTextColor(Color.parseColor(viewModel.getProyectoEstadoColor(proyecto.estado)))

        return view
    }

    /**
     * Configurar actividad reciente
     */
    private fun setupActividadReciente(actividades: List<ActividadReciente>) {
        binding.llActividadReciente.removeAllViews()

        actividades.forEach { actividad ->
            val actividadView = createActividadView(actividad)
            binding.llActividadReciente.addView(actividadView)
        }
    }

    /**
     * Crear vista de actividad reciente
     */
    private fun createActividadView(actividad: ActividadReciente): View {
        val textView = TextView(requireContext()).apply {
            text = "• ${actividad.accion}"
            textSize = 13f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, 8, 0, 8)
        }
        return textView
    }

    /**
     * Formatear fecha y hora
     */
    private fun formatDateTime(dateTimeStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr)
            date?.let { outputFormat.format(it) } ?: dateTimeStr
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    /**
     * Actualizar menú de navegación
     */
    private fun updateNavigationMenu(role: String) {
        (activity as? MainActivity)?.updateMenuVisibility(role)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}