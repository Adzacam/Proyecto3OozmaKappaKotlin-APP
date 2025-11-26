package com.example.develarqapp.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.Meeting
import com.example.develarqapp.data.model.MeetingRequest
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.model.User
import com.example.develarqapp.data.repository.CalendarRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel : ViewModel() {

    private val repository = CalendarRepository()

    private val _meetings = MutableLiveData<List<Meeting>>()
    val meetings: LiveData<List<Meeting>> = _meetings

    private val _filteredMeetings = MutableLiveData<List<Meeting>>()
    val filteredMeetings: LiveData<List<Meeting>> = _filteredMeetings

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _operationSuccess = MutableLiveData<Boolean>(false)
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    private var currentFilter = MeetingFilter.ALL
    private var selectedProjectId: Long? = null
    private var selectedUserId: Long? = null

    enum class MeetingFilter {
        ALL, UPCOMING, PAST
    }

    fun loadMeetings(token: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getMeetings(token)
                if (result.isSuccess) {
                    val meetingsList = result.getOrNull() ?: emptyList()
                    _meetings.value = meetingsList
                    applyFilters()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar reuniones"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProjects(token: String) {
        viewModelScope.launch {
            try {
                val result = repository.getProjects(token)
                if (result.isSuccess) {
                    _projects.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) { /* Error silencioso */ }
        }
    }

    fun loadUsers(token: String) {
        viewModelScope.launch {
            try {
                val result = repository.getUsers(token)
                if (result.isSuccess) {
                    _users.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) { /* Error silencioso */ }
        }
    }

    fun createMeeting(
        token: String,
        projectId: Long?,
        title: String,
        description: String?,
        startTime: String,
        endTime: String?,
        participantIds: List<Long>
    ) {
        if (title.trim().isEmpty()) {
            _errorMessage.value = "El título es requerido"
            return
        }
        if (projectId == null || projectId == 0L) {
            _errorMessage.value = "Debes seleccionar un proyecto"
            return
        }
        if (participantIds.isEmpty()) {
            _errorMessage.value = "Debes seleccionar al menos un participante"
            return
        }

        if (endTime != null && endTime.isNotEmpty()) {
            if (startTime >= endTime) {
                _errorMessage.value = "La fecha de fin debe ser posterior a la fecha de inicio"
                return
            }
        }

        _isLoading.value = true
        _operationSuccess.value = false

        val request = MeetingRequest(
            proyectoId = projectId,
            titulo = title.trim(),
            descripcion = description?.trim(),
            fechaHora = startTime,
            fechaHoraFin = endTime,
            participantes = participantIds
        )

        viewModelScope.launch {
            try {
                val result = repository.createMeeting(request, token)
                if (result.isSuccess) {
                    _successMessage.value = "Reunión creada exitosamente"
                    _operationSuccess.value = true
                    loadMeetings(token)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al crear la reunión"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMeeting(
        meetingId: Long,
        token: String,
        projectId: Long,
        title: String,
        description: String?,
        startTime: String,
        endTime: String?,
        participantIds: List<Long>
    ) {
        if (title.trim().isEmpty()) {
            _errorMessage.value = "El título es requerido"
            return
        }
        if (participantIds.isEmpty()) {
            _errorMessage.value = "Debes seleccionar al menos un participante"
            return
        }

        if (endTime != null && endTime.isNotEmpty()) {
            if (startTime >= endTime) {
                _errorMessage.value = "La fecha de fin debe ser posterior a la fecha de inicio"
                return
            }
        }

        _isLoading.value = true
        _operationSuccess.value = false

        val request = MeetingRequest(
            proyectoId = projectId,
            titulo = title.trim(),
            descripcion = description?.trim(),
            fechaHora = startTime,
            fechaHoraFin = endTime,
            participantes = participantIds
        )

        viewModelScope.launch {
            try {
                val result = repository.updateMeeting(meetingId, request, token)
                if (result.isSuccess) {
                    _successMessage.value = "Reunión actualizada exitosamente"
                    _operationSuccess.value = true
                    loadMeetings(token)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al actualizar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyFilters() {
        val allMeetings = _meetings.value ?: return
        val now = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        var filtered = allMeetings

        filtered = when (currentFilter) {
            MeetingFilter.UPCOMING -> {
                filtered.filter { meeting ->
                    try {
                        val meetingDate = dateFormat.parse(meeting.fechaHora)
                        meetingDate != null && meetingDate.after(now)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            MeetingFilter.PAST -> {
                filtered.filter { meeting ->
                    try {
                        val meetingDate = dateFormat.parse(meeting.fechaHora)
                        meetingDate != null && meetingDate.before(now)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            else -> filtered
        }

        selectedProjectId?.let { projectId ->
            filtered = filtered.filter { it.proyectoId == projectId }
        }

        selectedUserId?.let { userId ->
            filtered = filtered.filter { meeting ->
                meeting.participantes?.any { it.userId == userId } == true
            }
        }

        _filteredMeetings.value = filtered
    }

    fun deleteMeeting(meetingId: Long, token: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.deleteMeeting(meetingId, token)
                if (result.isSuccess) {
                    _successMessage.value = "Reunión eliminada"
                    loadMeetings(token)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTimeFilter(filter: MeetingFilter) {
        currentFilter = filter
        applyFilters()
    }

    fun filterByProject(projectId: Long?) {
        selectedProjectId = projectId
        applyFilters()
    }

    fun filterByUser(userId: Long?) {
        selectedUserId = userId
        applyFilters()
    }

    fun clearFilters() {
        currentFilter = MeetingFilter.ALL
        selectedProjectId = null
        selectedUserId = null
        applyFilters()
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccess() { _successMessage.value = null }
    fun resetOperationSuccess() { _operationSuccess.value = false }
}