package com.example.develarqapp.data.model

import com.example.develarqapp.data.model.Project
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Meeting(
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

    @SerializedName("fecha_hora")
    val fechaHora: String, // Formato: "2025-10-23 06:30:00"

    @SerializedName("fecha_hora_fin")
    val fechaHoraFin: String?,

    @SerializedName("creador_id")
    val creadorId: Long?,

    @SerializedName("participantes")
    val participantes: List<MeetingParticipant>? = null,

    @SerializedName("eliminado")
    val eliminado: Int = 0
): Serializable

data class MeetingParticipant(
    @SerializedName("id")
    val id: Long,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("asistio")
    val asistio: Int = 0
)

data class MeetingRequest(
    @SerializedName("proyecto_id")
    val proyectoId: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("fecha_hora")
    val fechaHora: String,

    @SerializedName("fecha_hora_fin")
    val fechaHoraFin: String?,

    @SerializedName("participantes")
    val participantes: List<Long>, // IDs de usuarios

    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("sdk_version")
    val sdkVersion: Int? = null
)

data class MeetingsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<Meeting>?
)

data class MeetingResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: Meeting?
)


