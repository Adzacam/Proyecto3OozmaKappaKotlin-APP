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