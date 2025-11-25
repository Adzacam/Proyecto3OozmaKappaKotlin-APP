package com.example.develarqapp.ui.documents

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentFilters
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.repository.DocumentRepository
import com.example.develarqapp.data.repository.ProjectRepository
import kotlinx.coroutines.launch
import java.io.File

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)
    private val projectRepository = ProjectRepository(application)

    // LiveData para documentos
    private val _documents = MutableLiveData<List<Document>>()
    val documents: LiveData<List<Document>> = _documents

    private val _filteredDocuments = MutableLiveData<List<Document>>()
    val filteredDocuments: LiveData<List<Document>> = _filteredDocuments

    private val _deletedDocuments = MutableLiveData<List<Document>>()
    val deletedDocuments: LiveData<List<Document>> = _deletedDocuments

    // LiveData para proyectos (para filtros)
    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    // LiveData para estados
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _downloadedFile = MutableLiveData<File>()
    val downloadedFile: LiveData<File> = _downloadedFile

    // Filtros
    private val _filters = MutableLiveData(DocumentFilters())
    val filters: LiveData<DocumentFilters> = _filters

    // ========== CARGAR DOCUMENTOS ==========

    fun loadDocuments(projectId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getDocuments(projectId)

            result.onSuccess { docs ->
                _documents.value = docs
                applyFilters()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al cargar documentos"
                _documents.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    fun loadDeletedDocuments() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getDeletedDocuments()

            result.onSuccess { docs ->
                _deletedDocuments.value = docs
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al cargar papelera"
                _deletedDocuments.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            val result = projectRepository.getProjects()

            result.onSuccess { projects ->
                _projects.value = projects
            }.onFailure {
                _projects.value = emptyList()
            }
        }
    }

    // ========== SUBIR DOCUMENTO ==========

    fun uploadDocument(
        projectId: Long,
        nombre: String,
        descripcion: String?,
        tipo: DocumentType,
        fileUri: Uri?,
        enlaceExterno: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // Validaciones
            val validationError = validateDocumentUpload(nombre, tipo, fileUri, enlaceExterno)
            if (validationError != null) {
                _errorMessage.value = validationError
                _isLoading.value = false
                return@launch
            }

            val result = repository.uploadDocument(
                projectId,
                nombre.trim(),
                descripcion?.trim(),
                tipo,
                fileUri,
                enlaceExterno?.trim()
            )

            _isLoading.value = false

            result.onSuccess { document ->
                _successMessage.value = "Documento subido exitosamente"
                // Recargar inmediatamente después del éxito
                loadDocuments()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al subir documento"
            }
        }
    }

    // ========== ACTUALIZAR DOCUMENTO ==========

    fun updateDocument(
        id: Long,
        nombre: String,
        descripcion: String?,
        projectId: Long?,
        tipo: DocumentType?,
        enlaceExterno: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            if (nombre.isBlank()) {
                _errorMessage.value = "El nombre del documento es requerido"
                _isLoading.value = false
                return@launch
            }

            val result = repository.updateDocument(
                id,
                nombre.trim(),
                descripcion?.trim(),
                projectId,
                tipo,
                enlaceExterno?.trim()
            )

            _isLoading.value = false

            result.onSuccess { message ->
                _successMessage.value = message
                // Recargar inmediatamente después del éxito
                loadDocuments()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al actualizar documento"
            }
        }
    }

    // ========== ELIMINAR DOCUMENTO ==========

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.deleteDocument(documentId)

            result.onSuccess { message ->
                _successMessage.value = message
                loadDocuments() // Recargar lista
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al eliminar documento"
            }

            _isLoading.value = false
        }
    }

    // ========== RESTAURAR DOCUMENTO ==========

    fun restoreDocument(documentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.restoreDocument(documentId)

            result.onSuccess { message ->
                _successMessage.value = message
                loadDeletedDocuments() // Recargar papelera
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al restaurar documento"
            }

            _isLoading.value = false
        }
    }

    // ========== DESCARGAR DOCUMENTO ==========

    fun downloadDocument(document: Document) {
        try {
            val fileName = document.nombre + getExtensionForType(document.tipo)

            // Llamamos al repositorio (que tampoco debería ser suspend si usa DownloadManager)
            val result = repository.downloadDocument(document.id, fileName)

            result.onSuccess { mensaje ->
                _successMessage.value = mensaje
            }.onFailure { error ->
                _errorMessage.value = "Error al iniciar descarga: ${error.message}"
            }

        } catch (e: Exception) {
            _errorMessage.value = "Error inesperado: ${e.message}"
        }
    }

    // ========== FILTROS ==========

    fun updateFilters(newFilters: DocumentFilters) {
        _filters.value = newFilters
        applyFilters()
    }

    fun clearFilters() {
        _filters.value = DocumentFilters()
        applyFilters()
    }

    private fun applyFilters() {
        val currentDocuments = _documents.value ?: emptyList()
        val currentFilters = _filters.value ?: DocumentFilters()

        if (currentFilters.isEmpty()) {
            _filteredDocuments.value = currentDocuments
            return
        }

        val filtered = currentDocuments.filter { document ->
            // Filtro por búsqueda
            val matchesSearch = if (currentFilters.searchQuery.isNotBlank()) {
                document.nombre.contains(currentFilters.searchQuery, ignoreCase = true) ||
                        document.descripcion?.contains(currentFilters.searchQuery, ignoreCase = true) == true
            } else true

            // Filtro por tipo
            val matchesType = if (currentFilters.selectedType != null) {
                document.tipo == currentFilters.selectedType
            } else true

            // Filtro por proyecto
            val matchesProject = if (currentFilters.selectedProjectId != null) {
                document.proyectoId == currentFilters.selectedProjectId
            } else true

            matchesSearch && matchesType && matchesProject
        }

        _filteredDocuments.value = filtered
    }

    // ========== VALIDACIONES ==========

    private fun validateDocumentUpload(
        nombre: String,
        tipo: DocumentType,
        fileUri: Uri?,
        enlaceExterno: String?
    ): String? {
        return when {
            nombre.isBlank() -> "El nombre del documento es requerido"
            nombre.length > 150 -> "El nombre no puede exceder 150 caracteres"
            tipo == DocumentType.URL && enlaceExterno.isNullOrBlank() ->
                "El enlace externo es requerido para este tipo"
            tipo == DocumentType.URL && !isValidUrl(enlaceExterno) ->
                "El enlace externo no es válido"
            tipo != DocumentType.URL && fileUri == null ->
                "El archivo es requerido"
            else -> null
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    private fun getExtensionForType(type: DocumentType): String {
        return when (type) {
            DocumentType.PDF -> ".pdf"
            DocumentType.EXCEL -> ".xlsx"
            DocumentType.WORD -> ".docx"
            DocumentType.URL -> ""
        }
    }

    // ========== LIMPIAR MENSAJES ==========

    fun clearMessages() {
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}