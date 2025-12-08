package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// Definición única de Project
data class Project(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha_inicio") val fechaInicio: String?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("cliente_id") val clienteId: Long?,
    @SerializedName("responsable_id") val responsableId: Long?
)

data class ProjectsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<Project>?,
    // Este campo es necesario para solucionar el error "Unresolved reference: message"
    @SerializedName("message") val message: String?
)

data class ProjectResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: Project?,

    @SerializedName("message")
    val message: String?
)

data class CreateProjectRequest(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("fecha_inicio")
    val fechaInicio: String,

    @SerializedName("fecha_fin")
    val fechaFin: String?,

    @SerializedName("cliente_id")
    val clienteId: Long,

    @SerializedName("responsable_id")
    val responsableId: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class UpdateProjectRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("fecha_inicio")
    val fechaInicio: String,

    @SerializedName("fecha_fin")
    val fechaFin: String?,

    @SerializedName("cliente_id")
    val clienteId: Long,

    @SerializedName("responsable_id")
    val responsableId: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)



// ============================================
// METADATA (para futuras extensiones)
// ============================================

data class ProjectMetadata(
    @SerializedName("total_documentos")
    val totalDocumentos: Int?,

    @SerializedName("total_planos_bim")
    val totalPlanosBim: Int?,

    @SerializedName("total_tareas")
    val totalTareas: Int?,

    @SerializedName("total_reuniones")
    val totalReuniones: Int?,

    @SerializedName("progreso_tareas")
    val progresoTareas: Int?
)