package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.DashboardStats

/**
 * Repositorio para manejar las operaciones del Dashboard
 *
 * Ubicación: app/src/main/java/com/example/develarqapp/data/repository/DashboardRepository.kt
 */
class DashboardRepository {

    private val apiService = ApiConfig.getApiService()

    /**
     * Obtener estadísticas del dashboard según el rol del usuario
     */
    suspend fun getDashboardStats(token: String): Result<DashboardStats> {
        return try {
            val response = apiService.getDashboardStats(
                token = "Bearer $token"
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val stats = response.body()!!.data
                if (stats != null) {
                    Result.success(stats)
                } else {
                    Result.failure(Exception("No se recibieron datos del dashboard"))
                }
            } else {
                Result.failure(Exception("Error al obtener estadísticas: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}