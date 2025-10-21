package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// --- Peticiones (Requests) ---
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
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

data class GenericResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)
//--- Modelo de Usuario ---
data class User(
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
) {
    val fullName: String
        get() = "$name $apellido"
}
//--- Modelo de Proyecto ---
data class Project(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("fecha_inicio")
    val fechaInicio: String?,

    @SerializedName("fecha_fin")
    val fechaFin: String?,

    @SerializedName("cliente_id")
    val clienteId: Long?,

    @SerializedName("responsable_id")
    val responsableId: Long?
)

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