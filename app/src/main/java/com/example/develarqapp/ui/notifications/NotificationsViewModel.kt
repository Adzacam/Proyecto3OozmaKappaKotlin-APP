package com.example.develarqapp.ui.notifications

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.Notification
import com.example.develarqapp.data.repository.NotificationsRepository
import com.example.develarqapp.utils.SessionManager
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val repository = NotificationsRepository()

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Para reproducir sonido
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Cargar notificaciones del usuario
     */
    fun loadNotifications(sessionManager: SessionManager) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val token = sessionManager.getAuthToken()

                if (userId != null && token != null) {
                    val result = repository.getNotifications(userId, token)

                    if (result.isSuccess) {
                        val notificationsList = result.getOrNull() ?: emptyList()
                        _notifications.value = notificationsList
                        _unreadCount.value = notificationsList.count { !it.isRead }
                    } else {
                        _errorMessage.value = "Error al cargar notificaciones"
                    }
                } else {
                    _errorMessage.value = "No se pudo obtener la sesión del usuario"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Marcar una notificación como leída
     */
    fun markAsRead(notificationId: Long, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                if (token != null) {
                    // Actualizar localmente primero (optimistic update)
                    val currentList = _notifications.value ?: return@launch
                    val updatedList = currentList.map {
                        if (it.id == notificationId) it.copy(leida = true, esNueva = false)
                        else it
                    }
                    _notifications.value = updatedList
                    _unreadCount.value = updatedList.count { !it.isRead }

                    // Actualizar en el servidor
                    val result = repository.markAsRead(notificationId, token)
                    if (!result.isSuccess) {
                        // Si falla, revertir el cambio
                        _notifications.value = currentList
                        _unreadCount.value = currentList.count { !it.isRead }
                        _errorMessage.value = "Error al marcar notificación"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al marcar notificación: ${e.message}"
            }
        }
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    fun markAllAsRead(sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                if (token != null) {
                    // Actualizar localmente primero
                    val currentList = _notifications.value ?: return@launch
                    val updatedList = currentList.map { it.copy(leida = true, esNueva = false) }
                    _notifications.value = updatedList
                    _unreadCount.value = 0

                    // Actualizar en el servidor
                    val result = repository.markAllAsRead(token)
                    if (result.isSuccess) {
                        _successMessage.value = "Todas las notificaciones marcadas como leídas"
                    } else {
                        // Si falla, revertir
                        _notifications.value = currentList
                        _unreadCount.value = currentList.count { !it.isRead }
                        _errorMessage.value = "Error al marcar todas las notificaciones"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al marcar todas: ${e.message}"
            }
        }
    }

    /**
     * Reproducir sonido de notificación
     */
    fun playNotificationSound(mediaPlayer: MediaPlayer?) {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
        } catch (e: Exception) {
            // Silenciosamente fallar si no se puede reproducir
            e.printStackTrace()
        }
    }

    /**
     * Verificar nuevas notificaciones comparando con la lista actual
     */
    fun checkForNewNotifications(newList: List<Notification>): Boolean {
        val currentList = _notifications.value ?: emptyList()
        val currentIds = currentList.map { it.id }.toSet()
        val hasNew = newList.any { it.id !in currentIds && it.isNew }
        return hasNew
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}