package com.example.develarqapp.data.repository

import android.content.Context
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.DownloadRecord
import com.example.develarqapp.utils.SessionManager

class DownloadHistoryRepository(context: Context) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)

    suspend fun getDownloadHistory(): Result<List<DownloadRecord>> {
        return try {
            val token = sessionManager.getToken()
                ?: return Result.failure(Exception("Token no disponible"))

            val response = apiService.getDownloadHistory("Bearer $token")

            if (response.isSuccessful && response.body()?.success == true) {
                val downloads = response.body()?.data ?: emptyList()
                Result.success(downloads)
            } else {
                // Si el modelo no tiene 'message', usamos uno genérico
                Result.failure(Exception("Error al cargar historial"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Cambiado Return Type a DownloadRecord
    suspend fun getFilteredHistory(
        userId: Long? = null,
        projectId: Long? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<DownloadRecord>> {
        return try {
            val token = sessionManager.getToken()
                ?: return Result.failure(Exception("Token no disponible"))

            val response = apiService.getDownloadHistoryFiltered(
                userId, projectId, startDate, endDate, "Bearer $token"
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val downloads = response.body()?.data ?: emptyList()
                Result.success(downloads)
            } else {
                Result.failure(Exception("Error al filtrar historial"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}