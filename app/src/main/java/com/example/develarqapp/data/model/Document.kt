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
    @SerializedName("PDF")
    PDF("Documento PDF", listOf(".pdf")),

    @SerializedName("Excel")
    EXCEL("Hoja de Cálculo", listOf(".xlsx", ".xls")),

    @SerializedName("Word")
    WORD("Documento Word", listOf(".docx", ".doc")),

    @SerializedName("URL")
    URL("Enlace Externo", listOf());

    companion object {
        fun fromMimeType(mimeType: String): DocumentType? {
            return when {
                mimeType.contains("pdf") -> PDF
                mimeType.contains("excel") || mimeType.contains("spreadsheet") -> EXCEL
                mimeType.contains("word") || mimeType.contains("document") -> WORD
                else -> null
            }
        }

        fun fromExtension(fileName: String): DocumentType? {
            return values().find { type ->
                type.extensions.any { ext -> fileName.endsWith(ext, ignoreCase = true) }
            }
        }
    }
}

// Response para lista de documentos
data class DocumentsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<Document>?
)

// Response para un documento
data class DocumentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Document?
)

// Request para subir documento
data class UploadDocumentRequest(
    @SerializedName("proyecto_id") val proyectoId: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("enlace_externo") val enlaceExterno: String?
)

// Request para actualizar documento
data class UpdateDocumentRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("proyecto_id") val proyectoId: Long?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("enlace_externo") val enlaceExterno: String?
)

// Request para eliminar documento
data class DeleteDocumentRequest(
    @SerializedName("id") val id: Long
)
// Eliminar 30 dias
data class PurgeResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("deleted_count") val deletedCount: Int?,
    @SerializedName("total_found") val totalFound: Int?,
    val errors: List<String>?
)

// Request genérica con ID
data class DocumentIdRequest(
    @SerializedName("id") val id: Long
)

// Filtros para búsqueda
data class DocumentFilters(
    var searchQuery: String = "",
    var selectedType: DocumentType? = null,
    var selectedProjectId: Long? = null
) {
    fun isEmpty(): Boolean {
        return searchQuery.isBlank() && selectedType == null && selectedProjectId == null
    }
}