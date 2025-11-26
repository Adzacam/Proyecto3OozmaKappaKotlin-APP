package com.example.develarqapp.ui.projects

import androidx.lifecycle.*
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.model.ChangeProjectStateRequest
import kotlinx.coroutines.launch

class ProyectosViewModel : ViewModel() {

    private val api = ApiConfig.getApiService()

    // Lista completa de proyectos
    private val _projects = MutableLiveData<List<Project>>(emptyList())
    val projects: LiveData<List<Project>> = _projects

    // Filtro seleccionado
    private val _filter = MutableLiveData<String>("todos")
    val filter: LiveData<String> = _filter

    // Lista filtrada (combinación reactiva)
    val filteredProjects = MediatorLiveData<List<Project>>().apply {
        addSource(_projects) { value = applyFilter(it, _filter.value) }
        addSource(_filter) { value = applyFilter(_projects.value, it) }
    }

    private fun applyFilter(list: List<Project>?, filtro: String?): List<Project> {
        if (list == null) return emptyList()
        return if (filtro == null || filtro == "todos") {
            list
        } else {
            list.filter { it.estado == filtro }
        }
    }

    // Estados de UI
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    /**
     * Cambiar filtro
     */
    fun setFilter(filtro: String) {
        _filter.value = filtro
    }

    /**
     * Cargar proyectos desde el backend
     */
    fun loadProjects() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val response = api.getProjects()
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        _projects.value = body.data ?: emptyList()
                    } else {
                        _error.value = body?.message ?: "Error al cargar proyectos"
                    }
                } else {
                    _error.value = "Error del servidor: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cambiar estado de un proyecto
     */
    fun changeProjectState(projectId: Long, nuevoEstado: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val request = ChangeProjectStateRequest(nuevoEstado)
                val response = api.changeProjectState(request, projectId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        _operationSuccess.value = "Estado actualizado correctamente"
                        
                        // Actualizar el estado localmente sin recargar todo
                        _projects.value = _projects.value?.map { project ->
                            if (project.id == projectId) {
                                project.copy(estado = nuevoEstado)
                            } else {
                                project
                            }
                        }
                    } else {
                        _error.value = body?.message ?: "Error al actualizar estado"
                    }
                } else {
                    _error.value = "Error del servidor: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar mensajes
     */
    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _operationSuccess.value = null
    }
}