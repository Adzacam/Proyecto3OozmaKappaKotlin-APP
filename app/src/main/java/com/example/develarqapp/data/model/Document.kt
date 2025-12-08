package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Modelo principal de Documento
data class Document(
    @SerializedName("id") val id: Long,
    @SerializedName("proyecto_id") val proyectoId: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("archivo_url") val archivoUrl: String,
    @SerializedName("enlace_externo") val enlaceExterno: String?,
    @SerializedName("tipo") val tipo: DocumentType,
    @SerializedName("fecha_subida") val fechaSubida: String,
    @SerializedName("subido_por") val subidoPor: Long?,
    @SerializedName("eliminado") val eliminado: Int = 0,
    @SerializedName("fecha_eliminacion") val fechaEliminacion: String?,
    @SerializedName("proyecto_nombre") val proyectoNombre: String? = null,
    @SerializedName("subido_por_nombre") val subidoPorNombre: String? = null
): Serializable {
    val isEliminado: Boolean
        get() = eliminado == 1
}

// Enum para tipos de documento
enum class DocumentType(val displayName: String, val extensions: List<String>) {
    @SerializedName("PDF") PDF("Documento PDF", listOf(".pdf")),
    @SerializedName("Excel") EXCEL("Hoja de Cálculo", listOf(".xlsx", ".xls")),
    @SerializedName("Word") WORD("Documento Word", listOf(".docx", ".doc")),
    @SerializedName("URL") URL("Enlace Externo", listOf());

    companion object {
        fun fromMimeType(mimeType: String): DocumentType? {
            return when {
                mimeType.contains("pdf") -> PDF
                mimeType.contains("excel") || mimeType.contains("spreadsheet") -> EXCEL
                mimeType.contains("word") || mimeType.contains("document") -> WORD
                else -> null
            }
        }
    }
}

// --- RESPUESTAS API (Aquí estaban los errores rojos) ---

data class DocumentsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<Document>?
)

data class DocumentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Document?
)

data class PurgeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("deleted_count") val deletedCount: Int?,
    @SerializedName("total_found") val totalFound: Int?,
    @SerializedName("errors") val errors: List<String>?
)

// --- REQUESTS API ---

data class UploadDocumentRequest(
    @SerializedName("proyecto_id") val proyectoId: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("enlace_externo") val enlaceExterno: String?
)

data class UpdateDocumentRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("proyecto_id") val proyectoId: Long?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("enlace_externo") val enlaceExterno: String?,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("android_version") val androidVersion: String? = null,
    @SerializedName("sdk_version") val sdkVersion: Int? = null
)

data class DeleteDocumentRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("android_version") val androidVersion: String? = null,
    @SerializedName("sdk_version") val sdkVersion: Int? = null
)

data class PurgeRequest(
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("android_version") val androidVersion: String? = null,
    @SerializedName("sdk_version") val sdkVersion: Int? = null
)

data class DocumentIdRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("android_version") val androidVersion: String? = null,
    @SerializedName("sdk_version") val sdkVersion: Int? = null
)


// Filtros
data class DocumentFilters(
    var searchQuery: String = "",
    var selectedType: DocumentType? = null,
    var selectedProjectId: Long? = null
) {
    fun isEmpty() = searchQuery.isBlank() && selectedType == null && selectedProjectId == null
}

//
data class DownloadRecord(
    @SerializedName("id") val id: Long,
    @SerializedName("documento") val documento: String,
    @SerializedName("usuario") val usuario: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("proyecto") val proyecto: String?
)

// GeneralResponse.kt
data class GeneralResponse(
    val success: Boolean,
    val message: String
)