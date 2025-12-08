package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// RESPONSE MODELS
// ============================================

data class BimPlanosResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<BimPlano>?,

    @SerializedName("message")
    val message: String?
)

data class BimPlanoResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: BimPlano?,

    @SerializedName("message")
    val message: String?
)

// ============================================
// DATA MODEL
// ============================================

data class BimPlano(
    @SerializedName("id")
    val id: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("tipo")
    val tipo: String, // PDF, GLB, FBX, Excel, JPG/PNG

    @SerializedName("archivo_url")
    val archivoUrl: String,

    @SerializedName("version")
    val version: String?,

    @SerializedName("fecha_subida")
    val fechaSubida: String,

    @SerializedName("proyecto_nombre")
    val proyectoNombre: String?,

    @SerializedName("proyecto_id")
    val proyectoId: Long?,

    @SerializedName("subido_por_nombre")
    val subidoPorNombre: String?,

    // Campos extra para papelera
    @SerializedName("eliminado_el")
    val eliminadoEl: String?,

    @SerializedName("dias_restantes")
    val diasRestantes: Int?
)

// ============================================
// REQUEST MODELS
// ============================================

data class UpdateBimPlanoRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class DeleteBimPlanoRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("motivo")
    val motivo: String,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)

data class BimPlanoIdRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)
// ============================================
// VERSIONES DE PLANOS
// ============================================

data class PlanoVersionsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<PlanoVersion>?,

    @SerializedName("total_versiones")
    val totalVersiones: Int?,

    @SerializedName("message")
    val message: String?
)

data class PlanoVersion(
    @SerializedName("id")
    val id: Long,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("archivo_url")
    val archivoUrl: String,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("fecha_subida")
    val fechaSubida: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("subido_por_nombre")
    val subidoPorNombre: String?,

    @SerializedName("subido_por_id")
    val subidoPorId: Long?,

    @SerializedName("es_version_actual")
    val esVersionActual: Boolean
)

data class SetVersionActualRequest(
    @SerializedName("version_id")
    val versionId: Long,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)
