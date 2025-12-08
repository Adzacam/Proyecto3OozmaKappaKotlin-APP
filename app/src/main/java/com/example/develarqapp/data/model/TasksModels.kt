package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// MODELOS DE TAREAS PARA KANBAN
// ============================================

data class TaskComplete(
    @SerializedName("id")
    val id: Long,

    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("proyecto_nombre")
    val proyectoNombre: String?,

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
    val asignadoNombre: String?,

    @SerializedName("creador_id")
    val creadorId: Long?,

    @SerializedName("creador_nombre")
    val creadorNombre: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
) {
    // Propiedades calculadas para UI
    val prioridadColor: Int
        get() = when (prioridad.lowercase()) {
            "alta" -> android.graphics.Color.parseColor("#EF4444")
            "media" -> android.graphics.Color.parseColor("#F59E0B")
            else -> android.graphics.Color.parseColor("#10B981")
        }

    val estadoColor: Int
        get() = when (estado.lowercase()) {
            "completado" -> android.graphics.Color.parseColor("#10B981")
            "en progreso" -> android.graphics.Color.parseColor("#3B82F6")
            else -> android.graphics.Color.parseColor("#6B7280")
        }
}

// ============================================
// REQUESTS - CREAR Y ACTUALIZAR TAREAS
// ============================================

data class CreateTaskRequest(
    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String = "pendiente",

    @SerializedName("prioridad")
    val prioridad: String,

    @SerializedName("fecha_limite")
    val fechaLimite: String?,

    @SerializedName("asignado_id")
    val asignadoId: Long?,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class UpdateTaskRequest(
    @SerializedName("tarea_id")
    val tareaId: Long,

    @SerializedName("titulo")
    val titulo: String?,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String?,

    @SerializedName("prioridad")
    val prioridad: String?,

    @SerializedName("fecha_limite")
    val fechaLimite: String?,

    @SerializedName("asignado_id")
    val asignadoId: Long?,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class UpdateTaskStateRequest(
    @SerializedName("tarea_id")
    val tareaId: Long,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class DeleteTaskRequest(
    @SerializedName("tarea_id")
    val tareaId: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

// ============================================
// RESPONSES
// ============================================

data class TasksResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<TaskComplete>
)

data class TaskResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: TaskComplete?
)

// ============================================
// MODELOS AUXILIARES
// ============================================

data class KanbanColumn(
    val estado: String,
    val titulo: String,
    val tareas: MutableList<TaskComplete> = mutableListOf()
)

data class ProjectSpinnerItem(
    val id: Long,
    val nombre: String
) {
    override fun toString(): String = nombre
}

data class UserSpinnerItem(
    val id: Long,
    val nombre: String
) {
    override fun toString(): String = nombre
}