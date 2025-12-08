package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// --- Peticiones (Requests) ---
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
)
// --- Respuestas (Responses) ---

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data") // Nos aseguramos de usar 'data' como en el ViewModel
    val data: UserData?,

    @SerializedName("token")
    val token: String?
)
// --- Objetos de Datos ---

data class UserData(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("apellido")
    val apellido: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("telefono")
    val telefono: String?,

    @SerializedName("rol")
    val rol: String,

    @SerializedName("estado")
    val estado: String
)

data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

data class LogoutRequest(
    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
)

data class GenericResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)
//--- Modelo de Usuario (Fusionado) ---
data class User(
    @SerializedName("id")
    val id: Long,

    // Campo del archivo 1
    @SerializedName("name")
    val name: String? = null,

    // Campo del archivo 2
    @SerializedName("nombre")
    val nombre: String? = null,

    // Hecho nulo para compatibilidad
    @SerializedName("apellido")
    val apellido: String? = null,

    // Hecho nulo para compatibilidad
    @SerializedName("email")
    val email: String? = null,

    // Campo del archivo 1
    @SerializedName("telefono")
    val telefono: String? = null,

    // Hecho nulo para compatibilidad
    @SerializedName("rol")
    val rol: String? = null,

    // Campo del archivo 1 (hecho nulo)
    @SerializedName("estado")
    val estado: String? = null,

    // Campo del archivo 2
    @SerializedName("eliminado")
    val eliminado: Int? = 0
) {
    // 'fullName' actualizado para usar 'name' o 'nombre'
    val fullName: String
        get() {
            val fName = name ?: nombre // Usa 'name' o 'nombre'
            val lName = apellido
            return if (!fName.isNullOrEmpty() && !lName.isNullOrEmpty()) {
                "$fName $lName"
            } else {
                email ?: "Usuario #$id"
            }
        }
}
//--- Modelo de Proyecto ---

//--- Modelo de Tarea ---
data class Task(
    @SerializedName("id")
    val id: Long,

    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String, // pendiente, en progreso, completado

    @SerializedName("prioridad")
    val prioridad: String, // baja, media, alta

    @SerializedName("fecha_limite")
    val fechaLimite: String?,

    @SerializedName("asignado_id")
    val asignadoId: Long?,

    @SerializedName("asignado_nombre")
    val asignadoNombre: String?
)

// ============================================
// Notificaciones
// ============================================

data class Notification(
    @SerializedName("id")
    val id: Long,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("tipo")
    val tipo: String, // documento, proyecto, tarea, reunion, hito, avance

    @SerializedName("asunto")
    val asunto: String?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("leida")
    val leida: Boolean,

    @SerializedName("es_nueva")
    val esNueva: Boolean,

    @SerializedName("fecha_envio")
    val fechaEnvio: String,

    @SerializedName("created_at")
    val createdAt: String
) {
    // Propiedades calculadas para la UI
    val title: String
        get() = asunto ?: "Notificación"

    val message: String
        get() = mensaje

    val type: String
        get() = tipo

    val isRead: Boolean
        get() = leida

    val isNew: Boolean
        get() = esNueva

    val date: String
        get() = formatDate(fechaEnvio)

    val projectName: String?
        get() = null // Se puede extraer del mensaje si es necesario

    private fun formatDate(dateString: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = sdf.parse(dateString)
            val now = java.util.Date()
            val diffInMillis = now.time - (date?.time ?: 0)
            val diffInHours = diffInMillis / (1000 * 60 * 60)

            when {
                diffInHours < 1 -> "Hace ${diffInMillis / (1000 * 60)} min"
                diffInHours < 24 -> "Hace ${diffInHours}h"
                else -> {
                    val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm", java.util.Locale.getDefault())
                    outputFormat.format(date!!)
                }
            }
        } catch (e: Exception) {
            dateString
        }
    }
}

data class NotificationsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<Notification>,

    @SerializedName("no_leidas")
    val noLeidas: Int,

    @SerializedName("total")
    val total: Int
)

data class MarkNotificationReadRequest(
    @SerializedName("notification_id")
    val notificationId: Long
)
// ============================================
// Dashboard Stats
// ============================================

data class UserInfo(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("rol")
    val rol: String
)

data class EstadoCount(
    @SerializedName("estado")
    val estado: String,

    @SerializedName("cantidad")
    val cantidad: Int
)
