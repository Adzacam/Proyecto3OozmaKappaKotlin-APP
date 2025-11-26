package com.example.develarqapp.data.repository

import android.content.Context
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.utils.SessionManager

class ProjectRepository(private val context: Context) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)

    // Helper para obtener el token
    private fun getAuthToken(): String {
        return "Bearer ${sessionManager.getToken()}"
    }

    // Obtener lista de proyectos
    suspend fun getProjects(): Result<List<Project>> {
        return try {
            val response = apiService.getProjects(getAuthToken())

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error desconocido al obtener proyectos"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}