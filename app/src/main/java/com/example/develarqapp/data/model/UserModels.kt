package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// Modelo completo de Usuario

// Request para crear usuario
data class CreateUserRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("apellido")
    val apellido: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("telefono")
    val telefono: String?,

    @SerializedName("rol")
    val rol: String
)

// Request para actualizar usuario
data class UpdateUserRequest(
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

    @SerializedName("password")
    val password: String?
)

// Request para eliminar/desactivar usuario
data class DeleteUserRequest(
    @SerializedName("id")
    val id: Long
)

// Response para lista de usuarios
data class UsersResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<User>?,

    @SerializedName("message")
    val message: String
)

// Response para operaciones de usuario individual
data class UserResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: User?,

    @SerializedName("message")
    val message: String
)
