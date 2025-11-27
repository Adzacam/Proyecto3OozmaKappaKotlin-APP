package com.example.develarqapp.data.model

import java.io.Serializable

data class DownloadHistory(
    val id: Long,
    val userId: Long,
    val userName: String,
    val documentoId: Long,
    val documentoNombre: String,
    val proyectoId: Long?,
    val proyectoNombre: String?,
    val fechaDescarga: String,
    val createdAt: String
) : Serializable

data class DownloadHistoryResponse(
    val success: Boolean,
    val message: String?,
    val data: List<DownloadHistory>?
)
data class RegisterDownloadRequest(
    val documento_id: Long
)