package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// RESPONSE MODEL
// ============================================

data class TimelineResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<TimelineEvent>?,

    @SerializedName("message")
    val message: String?
)

// ============================================
// DATA MODEL
// ============================================

data class TimelineEvent(
    @SerializedName("id")
    val id: Long,

    @SerializedName("tipo")
    val tipo: TipoEvento,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("usuario_nombre")
    val usuarioNombre: String?,

    @SerializedName("tabla_afectada")
    val tablaAfectada: String?,

    @SerializedName("registro_id")
    val registroId: Long?
)

enum class TipoEvento {
    @SerializedName("proyecto")
    PROYECTO,

    @SerializedName("documento")
    DOCUMENTO,

    @SerializedName("reunion")
    REUNION,

    @SerializedName("tarea")
    TAREA,

    @SerializedName("hito")
    HITO,

    @SerializedName("permiso")
    PERMISO,

    @SerializedName("auditoria")
    AUDITORIA,

    @SerializedName("bim")
    BIM
}