package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName
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
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<DownloadRecord>?
)


data class RegisterDownloadRequest(
    @SerializedName("documento_id") val documento_id: Long
)