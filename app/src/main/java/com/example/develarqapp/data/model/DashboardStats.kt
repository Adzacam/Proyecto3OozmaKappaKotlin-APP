package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para las estadísticas del Dashboard
 * Incluye información personal del usuario y estadísticas según su rol
 */
data class DashboardStats(
    @SerializedName("user_info")
    val userInfo: UserInfo? = null,

    // ========== ESTADÍSTICAS GENERALES ==========
    @SerializedName("total_proyectos")
    val totalProyectos: Int? = 0,

    @SerializedName("total_tareas")
    val totalTareas: Int? = 0,

    @SerializedName("reuniones_proximas")
    val reunionesProximas: Int? = 0,

    @SerializedName("total_documentos")
    val totalDocumentos: Int? = 0,

    @SerializedName("notificaciones_no_leidas")
    val notificacionesNoLeidas: Int? = 0,

    // ========== ESTADÍSTICAS DETALLADAS (ADMIN) ==========
    @SerializedName("usuarios_activos")
    val usuariosActivos: Int? = 0,

    @SerializedName("proyectos_por_estado")
    val proyectosPorEstado: List<EstadoCount>? = null,

    @SerializedName("tareas_por_estado")
    val tareasPorEstado: List<EstadoCount>? = null,

    @SerializedName("actividad_reciente")
    val actividadReciente: List<ActividadReciente>? = null,

    // ========== MIS ASIGNACIONES (ADMIN Y OTROS ROLES) ==========
    @SerializedName("mis_proyectos")
    val misProyectos: Int? = 0,

    @SerializedName("mis_tareas")
    val misTareas: Int? = 0,

    @SerializedName("mis_tareas_por_estado")
    val misTareasPorEstado: List<EstadoCount>? = null,

    @SerializedName("mis_reuniones")
    val misReuniones: Int? = 0,

    @SerializedName("proyectos_asignados")
    val proyectosAsignados: List<ProyectoResumen>? = null,

    @SerializedName("tareas_pendientes")
    val tareasPendientes: List<TareaResumen>? = null,

    @SerializedName("proximas_reuniones")
    val proximasReuniones: List<ReunionResumen>? = null
)

/**
 * Actividad reciente del sistema (solo para admin)
 */
data class ActividadReciente(
    @SerializedName("accion")
    val accion: String,

    @SerializedName("fecha_accion")
    val fechaAccion: String,

    @SerializedName("name")
    val userName: String? = null,

    @SerializedName("apellido")
    val userApellido: String? = null
)

/**
 * Resumen de proyecto para el dashboard
 */
data class ProyectoResumen(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("progreso")
    val progreso: Int? = 0,

    @SerializedName("fecha_inicio")
    val fechaInicio: String? = null,

    @SerializedName("fecha_fin")
    val fechaFin: String? = null
)

/**
 * Resumen de tarea para el dashboard
 */
data class TareaResumen(
    @SerializedName("id")
    val id: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("prioridad")
    val prioridad: String,

    @SerializedName("fecha_vencimiento")
    val fechaVencimiento: String? = null,

    @SerializedName("proyecto_nombre")
    val proyectoNombre: String? = null
)

/**
 * Resumen de reunión para el dashboard
 */
data class ReunionResumen(
    @SerializedName("id")
    val id: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("fecha_hora")
    val fechaHora: String,

    @SerializedName("proyecto_nombre")
    val proyectoNombre: String? = null,

    @SerializedName("ubicacion")
    val ubicacion: String? = null
)