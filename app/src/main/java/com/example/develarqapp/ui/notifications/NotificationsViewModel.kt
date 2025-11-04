package com.example.develarqapp.ui.notifications

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
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                // Actualizar localmente primero
                val currentList = _notifications.value ?: return@launch
                val updatedList = currentList.map {
                    if (it.id == notificationId) it.copy(isRead = true, isNew = false)
                    else it
                }
                _notifications.value = updatedList
                _unreadCount.value = updatedList.count { !it.isRead }

                // Luego actualizar en el servidor
                repository.markAsRead(notificationId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al marcar notificación: ${e.message}"
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val currentList = _notifications.value ?: return@launch
                val updatedList = currentList.map { it.copy(isRead = true, isNew = false) }
                _notifications.value = updatedList
                _unreadCount.value = 0

                // Actualizar en el servidor
                repository.markAllAsRead()
            } catch (e: Exception) {
                _errorMessage.value = "Error al marcar todas: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}