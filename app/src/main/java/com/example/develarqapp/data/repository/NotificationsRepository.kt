package com.example.develarqapp.data.repository

import com.example.develarqapp.data.model.Notification
import kotlinx.coroutines.delay

class NotificationsRepository {

    /**
     * Obtiene las notificaciones del usuario
     * TODO: Implementar llamada real a la API
     */
    suspend fun getNotifications(userId: Long, token: String): Result<List<Notification>> {
        return try {
            // Simular delay de red
            delay(500)

            // Datos de ejemplo - reemplazar con llamada real a API
            val mockNotifications = listOf(
                Notification(
                    id = 1,
                    type = "documento",
                    title = "Archivo BIM cargado",
                    message = "Se ha subido el primer archivo BIM del proyecto 'Mocca Master'.",
                    date = "28/10/2025, 8:57:53",
                    isRead = false,
                    isNew = true,
                    projectName = "Mocca Master"
                ),
                Notification(
                    id = 2,
                    type = "proyecto",
                    title = "Asignación de proyecto",
                    message = "Se te ha asignado el proyecto: Mocca Master",
                    date = "28/10/2025, 8:57:46",
                    isRead = false,
                    isNew = true,
                    projectName = "Mocca Master"
                ),
                Notification(
                    id = 3,
                    type = "proyecto",
                    title = "Estado del proyecto actualizado",
                    message = "El proyecto 'Cable HDMI' ha cambiado de estado a 'finalizado'.",
                    date = "28/10/2025, 8:32:28",
                    isRead = true,
                    isNew = false,
                    projectName = "Cable HDMI"
                ),
                Notification(
                    id = 4,
                    type = "proyecto",
                    title = "Asignación de proyecto",
                    message = "Se te ha asignado el proyecto: Cable HDMI",
                    date = "23/10/2025, 8:36:57",
                    isRead = true,
                    isNew = false,
                    projectName = "Cable HDMI"
                )
            )

            Result.success(mockNotifications)

            // TODO: Implementar llamada real
            /*
            val response = api.getNotifications(userId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al cargar notificaciones"))
            }
            */
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca una notificación como leída
     * TODO: Implementar llamada real a la API
     */
    suspend fun markAsRead(notificationId: Long): Result<Unit> {
        return try {
            delay(200)

            // TODO: Implementar llamada real
            /*
            val response = api.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al marcar notificación"))
            }
            */

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca todas las notificaciones como leídas
     * TODO: Implementar llamada real a la API
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            delay(300)

            // TODO: Implementar llamada real
            /*
            val response = api.markAllNotificationsAsRead()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al marcar todas"))
            }
            */

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}