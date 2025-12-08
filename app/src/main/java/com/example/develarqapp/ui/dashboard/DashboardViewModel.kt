package com.example.develarqapp.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.repository.DashboardRepository
import com.example.develarqapp.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = DashboardRepository()

    // Información del usuario
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    // Estadísticas completas
    private val _dashboardStats = MutableLiveData<DashboardStats>()
    val dashboardStats: LiveData<DashboardStats> = _dashboardStats

    // Estados de carga y error
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Cargar datos del usuario desde SessionManager
     */
    fun loadUserData(sessionManager: SessionManager) {
        _userName.value = sessionManager.getUserName()
        _userRole.value = sessionManager.getUserRol()
    }

    /**
     * Establecer datos del usuario manualmente
     */
    fun setUserData(name: String, role: String) {
        _userName.value = name
        _userRole.value = role
    }

    /**
     * Cargar estadísticas del dashboard según el rol
     */
    fun loadDashboardStats(sessionManager: SessionManager) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val token = sessionManager.getAuthToken()

                if (token != null) {
                    Log.d("DashboardViewModel", "🔄 Cargando estadísticas del dashboard...")

                    val result = repository.getDashboardStats(token)

                    if (result.isSuccess) {
                        val stats = result.getOrNull()
                        _dashboardStats.value = stats

                        Log.d("DashboardViewModel", "✅ Estadísticas cargadas correctamente")
                        logDashboardStats(stats)
                    } else {
                        val errorMsg = "Error al cargar estadísticas del dashboard"
                        _errorMessage.value = errorMsg
                        Log.e("DashboardViewModel", "❌ $errorMsg")
                    }
                } else {
                    val errorMsg = "No se pudo obtener el token de sesión"
                    _errorMessage.value = errorMsg
                    Log.e("DashboardViewModel", "❌ $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                _errorMessage.value = errorMsg
                Log.e("DashboardViewModel", "❌ Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Log detallado de las estadísticas recibidas
     */
    private fun logDashboardStats(stats: DashboardStats?) {
        stats?.let {
            Log.d("DashboardViewModel", "📊 === ESTADÍSTICAS RECIBIDAS ===")

            // User Info
            it.userInfo?.let { user ->
                Log.d("DashboardViewModel", "👤 Usuario: ${user.nombre} (${user.rol})")
            }

            // Estadísticas generales
            Log.d("DashboardViewModel", "📈 Total Proyectos: ${it.totalProyectos}")
            Log.d("DashboardViewModel", "📝 Total Tareas: ${it.totalTareas}")
            Log.d("DashboardViewModel", "📅 Reuniones Próximas: ${it.reunionesProximas}")
            Log.d("DashboardViewModel", "📄 Total Documentos: ${it.totalDocumentos}")

            // Mis asignaciones (si existen)
            it.misProyectos?.let { mp ->
                Log.d("DashboardViewModel", "📁 Mis Proyectos: $mp")
            }
            it.misTareas?.let { mt ->
                Log.d("DashboardViewModel", "✏️ Mis Tareas: $mt")
            }
            it.misReuniones?.let { mr ->
                Log.d("DashboardViewModel", "📆 Mis Reuniones: $mr")
            }

            // Admin específico
            it.usuariosActivos?.let { ua ->
                Log.d("DashboardViewModel", "👥 Usuarios Activos: $ua")
            }

            // Tareas por estado
            it.tareasPorEstado?.let { tpe ->
                Log.d("DashboardViewModel", "📊 Tareas por estado: ${tpe.size} estados")
                tpe.forEach { estado ->
                    Log.d("DashboardViewModel", "   - ${estado.estado}: ${estado.cantidad}")
                }
            }

            // Mis tareas por estado
            it.misTareasPorEstado?.let { mtpe ->
                Log.d("DashboardViewModel", "📊 Mis tareas por estado: ${mtpe.size} estados")
                mtpe.forEach { estado ->
                    Log.d("DashboardViewModel", "   - ${estado.estado}: ${estado.cantidad}")
                }
            }

            // Proyectos por estado
            it.proyectosPorEstado?.let { ppe ->
                Log.d("DashboardViewModel", "📊 Proyectos por estado: ${ppe.size} estados")
                ppe.forEach { estado ->
                    Log.d("DashboardViewModel", "   - ${estado.estado}: ${estado.cantidad}")
                }
            }

            // Tareas pendientes
            it.tareasPendientes?.let { tp ->
                Log.d("DashboardViewModel", "⚡ Tareas Pendientes: ${tp.size}")
            }

            // Reuniones próximas
            it.proximasReuniones?.let { pr ->
                Log.d("DashboardViewModel", "📅 Próximas Reuniones: ${pr.size}")
            }

            // Proyectos asignados
            it.proyectosAsignados?.let { pa ->
                Log.d("DashboardViewModel", "📂 Proyectos Asignados: ${pa.size}")
            }

            // Actividad reciente
            it.actividadReciente?.let { ar ->
                Log.d("DashboardViewModel", "🕐 Actividad Reciente: ${ar.size} eventos")
            }

            Log.d("DashboardViewModel", "📊 === FIN ESTADÍSTICAS ===")
        }
    }

    /**
     * Obtener color para un estado de proyecto
     */
    fun getProyectoEstadoColor(estado: String): String {
        return when (estado.lowercase()) {
            "activo", "en_progreso" -> "#10B981" // Verde
            "completado", "finalizado" -> "#3B82F6" // Azul
            "pausado" -> "#F59E0B" // Amarillo
            "cancelado" -> "#EF4444" // Rojo
            else -> "#64748B" // Gris
        }
    }

    /**
     * Obtener color para un estado de tarea
     */
    fun getTareaEstadoColor(estado: String): String {
        return when (estado.lowercase()) {
            "pendiente" -> "#F59E0B" // Amarillo
            "en_progreso" -> "#3B82F6" // Azul
            "completada", "completado" -> "#10B981" // Verde
            "cancelada", "cancelado" -> "#EF4444" // Rojo
            else -> "#64748B" // Gris
        }
    }

    /**
     * Obtener color para prioridad de tarea
     */
    fun getTareaPrioridadColor(prioridad: String): String {
        return when (prioridad.lowercase()) {
            "alta" -> "#EF4444" // Rojo
            "media" -> "#F59E0B" // Amarillo
            "baja" -> "#10B981" // Verde
            else -> "#64748B" // Gris
        }
    }

    /**
     * Calcular porcentaje para gráficas
     */
    fun calcularPorcentaje(cantidad: Int, total: Int): Float {
        return if (total > 0) {
            (cantidad.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refrescar estadísticas
     */
    fun refreshStats(sessionManager: SessionManager) {
        Log.d("DashboardViewModel", "🔄 Refrescando estadísticas...")
        loadDashboardStats(sessionManager)
    }
}