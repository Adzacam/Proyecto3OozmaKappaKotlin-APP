package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.AuditoriaLog

class AuditoriaRepository {

    private val api = ApiConfig.getApiService()

    suspend fun getAuditLogs(token: String): Result<List<AuditoriaLog>> {
        return try {

            val response = api.getAuditLogs("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener logs: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}