package com.example.develarqapp.data.repository

import android.content.Context
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.SessionManager

class ProjectRepository(private val context: Context) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)

    private fun getAuthToken(): String {
        return "Bearer ${sessionManager.getToken()}"
    }

    suspend fun getProjects(): Result<List<Project>> {
        return try {
            val response = apiService.getProjects(getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al obtener proyectos"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createProject(request: CreateProjectRequest): Result<Boolean> {
        return try {
            val response = apiService.createProject(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al crear proyecto"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateProject(request: UpdateProjectRequest): Result<Boolean> {
        return try {
            val response = apiService.updateProject(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al actualizar proyecto"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- HITOS ---
    suspend fun getHitos(projectId: Long): Result<List<Hito>> {
        return try {
            val response = apiService.getHitos(projectId, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar hitos"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createHito(request: CreateHitoRequest): Result<Boolean> {
        return try {
            val response = apiService.createHito(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al crear hito"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateHito(request: UpdateHitoRequest): Result<Boolean> {
        return try {
            val response = apiService.updateHito(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al actualizar hito"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteHito(request: DeleteHitoRequest): Result<Boolean> {
        return try {
            val response = apiService.deleteHito(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al eliminar hito"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- PERMISOS ---
    suspend fun getPermissions(projectId: Long): Result<List<ProjectPermission>> {
        return try {
            val response = apiService.getProjectPermissions(projectId, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar permisos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePermissions(request: UpdatePermissionsRequest): Result<Boolean> {
        return try {
            val response = apiService.updateProjectPermissions(request, getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al actualizar permisos"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- USUARIOS ---
    suspend fun getUsers(): Result<List<User>> {
        return try {
            val response = apiService.getUsers(getAuthToken())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Error al cargar usuarios"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- TIMELINE ---
    suspend fun getTimeline(projectId: Long): Result<List<TimelineEvent>> {
        // Implementar llamada a API cuando tengas el endpoint
        return Result.success(emptyList())
    }
}