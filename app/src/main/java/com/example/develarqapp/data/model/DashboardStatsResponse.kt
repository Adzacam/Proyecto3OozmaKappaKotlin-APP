package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del API para las estadísticas del Dashboard
 */
data class DashboardStatsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: DashboardStats? = null
)