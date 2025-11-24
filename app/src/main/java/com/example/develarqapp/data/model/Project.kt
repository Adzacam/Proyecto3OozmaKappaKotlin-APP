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