package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ==========================================
// MODELO PRINCIPAL DE PROYECTO
// ==========================================
data class Project(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("nombre")
    val nombre: String,
    
    @SerializedName("descripcion")
    val descripcion: String?,
    
    @SerializedName("estado")
    val estado: String, // "activo", "en progreso", "finalizado"
    
    @SerializedName("fecha_inicio")
    val fechaInicio: String?,
    
    @SerializedName("fecha_fin")
    val fechaFin: String?,
    
    @SerializedName("cliente_id")
    val clienteId: Long?,
    
    @SerializedName("responsable_id")
    val responsableId: Long?,
    
    // Relaciones (desde el backend)
    @SerializedName("cliente")
    val cliente: ClienteInfo? = null,
    
    @SerializedName("responsable")
    val responsable: ResponsableInfo? = null,
    
    // Flag para saber si el usuario actual es responsable
    @SerializedName("es_responsable")
    val esResponsable: Boolean = false
)

data class ClienteInfo(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String
)

data class ResponsableInfo(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String
)

// ==========================================
// REQUESTS
// ==========================================

/**
 * Request para cambiar estado de proyecto
 */
data class ChangeProjectStateRequest(
    @SerializedName("estado")
    val estado: String
)

/**
 * Response genérica de la API
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: T?
)