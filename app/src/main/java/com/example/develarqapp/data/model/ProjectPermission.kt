package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// RESPONSE MODEL
// ============================================

data class PermissionsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<ProjectPermission>?,

    @SerializedName("message")
    val message: String?
)

// ============================================
// DATA MODEL
// ============================================

data class ProjectPermission(
    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("user_nombre")
    val userNombre: String,

    @SerializedName("permiso")
    val permiso: TipoPermiso,

    @SerializedName("rol_en_proyecto")
    val rolEnProyecto: String?
)

enum class TipoPermiso {
    @SerializedName("editar")
    EDITAR,

    @SerializedName("ninguno")
    NINGUNO
}

// ============================================
// REQUEST MODEL
// ============================================

data class UpdatePermissionsRequest(
    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("permisos")
    val permisos: List<PermisoItem>,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class PermisoItem(
    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("permiso")
    val permiso: String
)