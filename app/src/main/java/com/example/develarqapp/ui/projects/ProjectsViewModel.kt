package com.example.develarqapp.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.repository.ProjectRepository
import com.example.develarqapp.utils.AuditManager
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    // Estado de la lista de proyectos
    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    // Estado de la lista de usuarios
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    // Estado de hitos
    private val _hitos = MutableLiveData<List<Hito>>()
    val hitos: LiveData<List<Hito>> = _hitos

    // Estado de permisos
    private val _permissions = MutableLiveData<List<ProjectPermission>>()
    val permissions: LiveData<List<ProjectPermission>> = _permissions

    // Estado de timeline
    private val _timeline = MutableLiveData<List<TimelineEvent>>()
    val timeline: LiveData<List<TimelineEvent>> = _timeline

    // Estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Manejo de errores y éxito
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    // ✅ NUEVO: Variable para controlar cierre de diálogos
    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    fun resetOperationSuccess() {
        _operationSuccess.value = false
    }

    // ============================================
    // PROYECTOS
    // ============================================

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getProjects()
            result.onSuccess { list -> _projects.value = list }
                .onFailure { err -> _errorMessage.value = err.message ?: "Error" }
            _isLoading.value = false
        }
    }

    fun createProject(
        nombre: String, descripcion: String?, estado: String,
        fechaInicio: String, fechaFin: String?,
        clienteId: Long, responsableId: Long
    ) {
        _isLoading.value = true
        _operationSuccess.value = false // Reset

        val deviceInfo = AuditManager.getDeviceInfo()
        val request = CreateProjectRequest(
            nombre, descripcion, estado, fechaInicio, fechaFin, clienteId, responsableId,
            deviceInfo["device_model"] as String,
            deviceInfo["android_version"] as String,
            deviceInfo["sdk_version"] as Int
        )

        viewModelScope.launch {
            val result = repository.createProject(request)
            result.onSuccess {
                _successMessage.value = "Proyecto creado exitosamente"
                _operationSuccess.value = true // ✅ Cierre
                loadProjects()
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun updateProject(
        id: Long, nombre: String, descripcion: String?, estado: String,
        fechaInicio: String, fechaFin: String?,
        clienteId: Long, responsableId: Long
    ) {
        _isLoading.value = true
        _operationSuccess.value = false

        val deviceInfo = AuditManager.getDeviceInfo()
        val request = UpdateProjectRequest(
            id, nombre, descripcion, estado, fechaInicio, fechaFin, clienteId, responsableId,
            deviceInfo["device_model"] as String,
            deviceInfo["android_version"] as String,
            deviceInfo["sdk_version"] as Int
        )

        viewModelScope.launch {
            val result = repository.updateProject(request)
            result.onSuccess {
                _successMessage.value = "Proyecto actualizado"
                _operationSuccess.value = true // ✅ Cierre
                loadProjects()
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    // ============================================
    // HITOS
    // ============================================

    fun loadProjectHitos(projectId: Long, estado: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getHitos(projectId)
            result.onSuccess { list ->
                // Filtrado local si es necesario, o pasar al repo
                val filtered = if (estado != null) list.filter { it.estado.name.equals(estado, true) } else list
                _hitos.value = filtered
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun createHito(proyectoId: Long, nombre: String, descripcion: String?, fechaHito: String, estado: String, encargadoId: Long?) {
        _isLoading.value = true
        _operationSuccess.value = false

        val deviceInfo = AuditManager.getDeviceInfo()
        val request = CreateHitoRequest(
            proyectoId, nombre, fechaHito, descripcion, estado, encargadoId,
            deviceInfo["device_model"] as String,
            deviceInfo["android_version"] as String,
            deviceInfo["sdk_version"] as Int
        )

        viewModelScope.launch {
            val result = repository.createHito(request)
            result.onSuccess {
                _successMessage.value = "Hito creado"
                _operationSuccess.value = true // ✅ Cierre
                loadProjectHitos(proyectoId)
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun updateHito(id: Long, nombre: String, descripcion: String?, fechaHito: String, estado: String, encargadoId: Long?) {
        _isLoading.value = true
        _operationSuccess.value = false

        val deviceInfo = AuditManager.getDeviceInfo()
        val request = UpdateHitoRequest(
            id, nombre, fechaHito, descripcion, estado, encargadoId,
            deviceInfo["device_model"] as String,
            deviceInfo["android_version"] as String,
            deviceInfo["sdk_version"] as Int
        )

        viewModelScope.launch {
            val result = repository.updateHito(request)
            result.onSuccess {
                _successMessage.value = "Hito actualizado"
                _operationSuccess.value = true // ✅ Cierre
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun deleteHito(id: Long) {
        _isLoading.value = true
        val deviceInfo = AuditManager.getDeviceInfo()
        val request = DeleteHitoRequest(id, deviceInfo["device_model"] as String, deviceInfo["android_version"] as String, deviceInfo["sdk_version"] as Int)

        viewModelScope.launch {
            val result = repository.deleteHito(request)
            result.onSuccess {
                _successMessage.value = "Hito eliminado"
                // No necesitamos cerrar diálogo aquí normalmente, pero si fuera necesario se usaría operationSuccess
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    // ============================================
    // USUARIOS Y PERMISOS
    // ============================================

    fun loadUsers() {
        viewModelScope.launch {
            val result = repository.getUsers()
            result.onSuccess { _users.value = it }
        }
    }

    fun loadProjectPermissions(projectId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getPermissions(projectId)
            result.onSuccess { _permissions.value = it }
                .onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun updateProjectPermissions(projectId: Long, permissionsMap: Map<Long, String>) {
        _isLoading.value = true
        _operationSuccess.value = false

        val permisoItems = permissionsMap.map { PermisoItem(it.key, it.value) }
        val deviceInfo = AuditManager.getDeviceInfo()
        val request = UpdatePermissionsRequest(
            projectId, permisoItems,
            deviceInfo["device_model"] as String,
            deviceInfo["android_version"] as String,
            deviceInfo["sdk_version"] as Int
        )

        viewModelScope.launch {
            val result = repository.updatePermissions(request)
            result.onSuccess {
                _successMessage.value = "Permisos actualizados"
                _operationSuccess.value = true // ✅ Cierre
            }.onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    // ============================================
    // TIMELINE
    // ============================================
    fun loadProjectTimeline(projectId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTimeline(projectId)
            result.onSuccess { _timeline.value = it }
                .onFailure { err -> _errorMessage.value = err.message }
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}