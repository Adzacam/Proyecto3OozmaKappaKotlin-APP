package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// RESPONSE MODELS
// ============================================

data class HitosResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<Hito>?,

    @SerializedName("message")
    val message: String?
)

data class HitoResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: Hito?,

    @SerializedName("message")
    val message: String?
)

// ============================================
// DATA MODEL
// ============================================

data class Hito(
    @SerializedName("id")
    val id: Long,

    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("fecha_hito")
    val fechaHito: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: EstadoHito,

    @SerializedName("encargado_id")
    val encargadoId: Long?,

    @SerializedName("encargado_nombre")
    val encargadoNombre: String?,

    @SerializedName("documento_id")
    val documentoId: Long?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)

enum class EstadoHito {
    @SerializedName("Pendiente")
    PENDIENTE,

    @SerializedName("En Progreso")
    EN_PROGRESO,

    @SerializedName("Completado")
    COMPLETADO,

    @SerializedName("Bloqueado")
    BLOQUEADO
}

// ============================================
// REQUEST MODELS
// ============================================

data class CreateHitoRequest(
    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("fecha_hito")
    val fechaHito: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("encargado_id")
    val encargadoId: Long?,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class UpdateHitoRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("fecha_hito")
    val fechaHito: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("encargado_id")
    val encargadoId: Long?,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class DeleteHitoRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)
