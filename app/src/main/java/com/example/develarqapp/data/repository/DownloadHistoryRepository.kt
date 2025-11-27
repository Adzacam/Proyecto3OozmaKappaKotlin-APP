package com.example.develarqapp.data.repository

import android.app.Application
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.DownloadHistory
import com.example.develarqapp.data.model.RegisterDownloadRequest
import com.example.develarqapp.utils.SessionManager


class DownloadHistoryRepository(application: Application) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(application)

    suspend fun getDownloadHistory(): Result<List<DownloadHistory>> {
        return try {
            val token = sessionManager.getToken()
                ?: return Result.failure(Exception("Token no disponible"))

            val response = apiService.getDownloadHistory("Bearer $token")

            if (response.isSuccessful && response.body()?.success == true) {
                val downloads = response.body()?.data ?: emptyList()
                Result.success(downloads)
            } else {
                val errorMsg = response.body()?.message ?: "Error al cargar historial"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFilteredHistory(
        userId: Long? = null,
        projectId: Long? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<DownloadHistory>> {
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
                val errorMsg = response.body()?.message ?: "Error al filtrar historial"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}