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
data class Project(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    // Hecho nulo para compatibilidad
    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("fecha_inicio")
    val fechaInicio: String? = null,

    @SerializedName("fecha_fin")
    val fechaFin: String? = null,

    @SerializedName("cliente_id")
    val clienteId: Long? = null,

    @SerializedName("responsable_id")
    val responsableId: Long? = null,

    // Campo del archivo 2
    @SerializedName("eliminado")
    val eliminado: Int? = 0
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

