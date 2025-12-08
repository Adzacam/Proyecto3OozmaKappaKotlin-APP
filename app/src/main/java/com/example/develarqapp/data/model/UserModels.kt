package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// Request mejorado para eliminar usuario con motivo y device info
data class DeleteUserRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("motivo")
    val motivo: String? = null,

    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
)

// Response específico para validar proyectos activos
data class DeleteUserResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("has_active_projects")
    val hasActiveProjects: Boolean? = null
)

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
    val rol: String,

    // Campos auditoría
    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
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
    val password: String?,

    // Campos auditoría
    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
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