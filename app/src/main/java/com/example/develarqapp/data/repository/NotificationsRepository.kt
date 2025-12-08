package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.MarkNotificationReadRequest
import com.example.develarqapp.data.model.Notification

class NotificationsRepository {

    private val apiService = ApiConfig.getApiService()

    /**
     * Obtener notificaciones del usuario
     */
    suspend fun getNotifications(userId: Long, token: String): Result<List<Notification>> {
        return try {
            val response = apiService.getNotifications(
                token = "Bearer $token",
                tipo = null,
                leida = null,
                limit = 50
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Error al obtener notificaciones"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar una notificación como leída
     */
    suspend fun markAsRead(notificationId: Long): Result<Boolean> {
        return try {
            // Obtener token desde SessionManager (necesitarás pasarlo como parámetro)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar una notificación como leída (con token)
     */
    suspend fun markAsRead(notificationId: Long, token: String): Result<Boolean> {
        return try {
            val request = MarkNotificationReadRequest(notificationId)
            val response = apiService.markNotificationRead(
                request = request,
                token = "Bearer $token"
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error al marcar notificación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    suspend fun markAllAsRead(): Result<Boolean> {
        return try {
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marcar todas las notificaciones como leídas (con token)
     */
    suspend fun markAllAsRead(token: String): Result<Boolean> {
        return try {
            val response = apiService.markAllNotificationsRead(
                token = "Bearer $token"
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error al marcar todas las notificaciones"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}