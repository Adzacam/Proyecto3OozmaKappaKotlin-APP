package com.example.develarqapp.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.repository.ProjectRepository
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    // Estado de la lista de proyectos
    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    // Estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Manejo de errores
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Inicializar cargando datos
    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getProjects()

            result.onSuccess { projectList ->
                _projects.value = projectList
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error de conexión"
                _projects.value = emptyList() // Limpiar lista en caso de error
            }

            _isLoading.value = false
        }
    }
}