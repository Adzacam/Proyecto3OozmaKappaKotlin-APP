package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.develarqapp.utils.AuditManager

class CalendarRepository {

    private val api = ApiConfig.getApiService()

    /**
     * Obtener todas las reuniones
     */
    suspend fun getMeetings(token: String): Result<List<Meeting>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMeetings("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val meetingsResponse = response.body()!!
                    if (meetingsResponse.success) {
                        Result.success(meetingsResponse.data ?: emptyList())
                    } else {
                        Result.failure(Exception(meetingsResponse.message ?: "Error al cargar reuniones"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener reuniones por proyecto
     */
    suspend fun getMeetingsByProject(projectId: Long, token: String): Result<List<Meeting>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMeetingsByProject(projectId, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val meetingsResponse = response.body()!!
                    if (meetingsResponse.success) {
                        Result.success(meetingsResponse.data ?: emptyList())
                    } else {
                        Result.failure(Exception(meetingsResponse.message ?: "Error"))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Crear nueva reunión
     */
    suspend fun createMeeting(meeting: MeetingRequest, token: String): Result<Meeting> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createMeeting(meeting, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val meetingResponse = response.body()!!
                    if (meetingResponse.success && meetingResponse.data != null) {
                        Result.success(meetingResponse.data)
                    } else {
                        Result.failure(Exception(meetingResponse.message ?: "Error al crear reunión"))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Actualizar reunión
     */
    suspend fun updateMeeting(meetingId: Long, meeting: MeetingRequest, token: String): Result<Meeting> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateMeeting(meetingId, meeting, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val meetingResponse = response.body()!!
                    if (meetingResponse.success && meetingResponse.data != null) {
                        Result.success(meetingResponse.data)
                    } else {
                        Result.failure(Exception(meetingResponse.message ?: "Error al actualizar"))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Eliminar reunión
     */
    suspend fun deleteMeeting(meetingId: Long, token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ Usar AuditManager
                val deviceInfo = AuditManager.getDeviceInfo()

                val response = api.deleteMeeting(meetingId, deviceInfo, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (genericResponse.success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(genericResponse.message))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener proyectos para filtro
     */
    suspend fun getProjects(token: String): Result<List<Project>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProjects("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val projectsResponse = response.body()!!
                    if (projectsResponse.success) {
                        Result.success(projectsResponse.data ?: emptyList())
                    } else {
                        Result.failure(Exception("Error al cargar proyectos"))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener usuarios para filtro de participantes
     */
    suspend fun getUsers(token: String): Result<List<com.example.develarqapp.data.model.User>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getUsers("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val usersResponse = response.body()!!
                    if (usersResponse.success) {
                        Result.success(usersResponse.data ?: emptyList())
                    } else {
                        Result.failure(Exception("Error al cargar usuarios"))
                    }
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}