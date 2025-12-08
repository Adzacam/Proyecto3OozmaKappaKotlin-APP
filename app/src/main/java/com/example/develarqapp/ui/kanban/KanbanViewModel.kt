package com.example.develarqapp.ui.kanban

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.SessionManager
import kotlinx.coroutines.launch

class KanbanViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(application)

    // ============================================
    // LiveData - Estados observables
    // ============================================

    private val _tasks = MutableLiveData<List<TaskComplete>>()
    val tasks: LiveData<List<TaskComplete>> = _tasks

    private val _kanbanColumns = MutableLiveData<List<KanbanColumn>>()
    val kanbanColumns: LiveData<List<KanbanColumn>> = _kanbanColumns

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _showEmptyState = MutableLiveData<Boolean>()
    val showEmptyState: LiveData<Boolean> = _showEmptyState

    // Proyecto seleccionado actualmente
    private val _selectedProjectId = MutableLiveData<Long?>()
    val selectedProjectId: LiveData<Long?> = _selectedProjectId

    // Usuario seleccionado como filtro
    private val _selectedUserId = MutableLiveData<Long?>()
    val selectedUserId: LiveData<Long?> = _selectedUserId

    // ============================================
    // Métodos principales
    // ============================================

    /**
     * Cargar proyectos según el rol del usuario
     */
    fun loadProjects() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId()
        val userRole = sessionManager.getUserRol()

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getProjects("Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    val allProjects = response.body()?.data ?: emptyList()

                    // Filtrar proyectos según rol
                    val filteredProjects = when (userRole.lowercase()) {
                        "admin" -> allProjects
                        else -> allProjects.filter { project ->
                            project.responsableId == userId || project.clienteId == userId
                        }
                    }

                    _projects.value = filteredProjects
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al cargar proyectos"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar usuarios para el spinner de responsables
     */
    fun loadUsers() {
        val token = sessionManager.getToken() ?: return

        viewModelScope.launch {
            try {
                val response = apiService.getUsers("Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _users.value = response.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {

            }
        }
    }

    /**
     * Cargar tareas del proyecto seleccionado
     */
    fun loadTasks(projectId: Long?, userId: Long? = null) {
        val token = sessionManager.getToken() ?: return
        if (projectId == null || projectId == 0L) {
            _showEmptyState.value = true
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _showEmptyState.value = false

                val response = apiService.getTasks(
                    token = "Bearer $token",
                    projectId = projectId,
                    asignadoId = userId
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val tasksList = response.body()?.data ?: emptyList()
                    _tasks.value = tasksList

                    // Organizar tareas en columnas Kanban
                    organizeKanbanColumns(tasksList)

                    _showEmptyState.value = false
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al cargar tareas"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Organizar tareas en columnas Kanban
     */
    private fun organizeKanbanColumns(tasksList: List<TaskComplete>) {
        val columns = listOf(
            KanbanColumn("pendiente", "Pendiente"),
            KanbanColumn("en progreso", "En Progreso"),
            KanbanColumn("completado", "Completado")
        )

        tasksList.forEach { task ->
            val column = columns.find { it.estado.equals(task.estado, ignoreCase = true) }
            column?.tareas?.add(task)
        }

        _kanbanColumns.value = columns
    }

    /**
     * Crear nueva tarea
     */
    fun createTask(
        projectId: Long,
        titulo: String,
        descripcion: String?,
        prioridad: String,
        fechaLimite: String?,
        asignadoId: Long?
    ) {
        val token = sessionManager.getToken() ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val request = CreateTaskRequest(
                    proyectoId = projectId,
                    titulo = titulo,
                    descripcion = descripcion,
                    prioridad = prioridad,
                    fechaLimite = fechaLimite,
                    asignadoId = asignadoId,
                    deviceModel = Build.MODEL,
                    androidVersion = Build.VERSION.RELEASE,
                    sdkVersion = Build.VERSION.SDK_INT
                )

                val response = apiService.createTask(request, "Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = "Tarea creada exitosamente"
                    loadTasks(_selectedProjectId.value, _selectedUserId.value)
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al crear tarea"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar estado de una tarea (drag & drop)
     */
    fun updateTaskState(taskId: Long, newState: String) {
        val token = sessionManager.getToken() ?: return

        viewModelScope.launch {
            try {
                val request = UpdateTaskStateRequest(
                    tareaId = taskId,
                    estado = newState,
                    deviceModel = Build.MODEL,
                    androidVersion = Build.VERSION.RELEASE,
                    sdkVersion = Build.VERSION.SDK_INT
                )

                val response = apiService.updateTaskState(request, "Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = "Estado actualizado"
                    loadTasks(_selectedProjectId.value, _selectedUserId.value)
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al actualizar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    /**
     * Actualizar tarea completa
     */
    fun updateTask(
        taskId: Long,
        titulo: String?,
        descripcion: String?,
        prioridad: String?,
        fechaLimite: String?,
        asignadoId: Long?
    ) {
        val token = sessionManager.getToken() ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val request = UpdateTaskRequest(
                    tareaId = taskId,
                    titulo = titulo,
                    descripcion = descripcion,
                    estado = null, // No cambiamos estado aquí
                    prioridad = prioridad,
                    fechaLimite = fechaLimite,
                    asignadoId = asignadoId,
                    deviceModel = Build.MODEL,
                    androidVersion = Build.VERSION.RELEASE,
                    sdkVersion = Build.VERSION.SDK_INT
                )

                val response = apiService.updateTask(request, "Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = "Tarea actualizada exitosamente"
                    loadTasks(_selectedProjectId.value, _selectedUserId.value)
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al actualizar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Eliminar tarea
     */
    fun deleteTask(taskId: Long) {
        val token = sessionManager.getToken() ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val request = DeleteTaskRequest(
                    tareaId = taskId,
                    deviceModel = Build.MODEL,
                    androidVersion = Build.VERSION.RELEASE,
                    sdkVersion = Build.VERSION.SDK_INT
                )

                val response = apiService.deleteTask(request, "Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = "Tarea eliminada"
                    loadTasks(_selectedProjectId.value, _selectedUserId.value)
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al eliminar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // Setters para filtros
    // ============================================

    fun setSelectedProject(projectId: Long?) {
        _selectedProjectId.value = projectId
        if (projectId != null) {
            loadTasks(projectId, _selectedUserId.value)
        }
    }

    fun setSelectedUser(userId: Long?) {
        _selectedUserId.value = userId
        _selectedProjectId.value?.let { projectId ->
            loadTasks(projectId, userId)
        }
    }

    // ============================================
    // Reset mensajes
    // ============================================

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}