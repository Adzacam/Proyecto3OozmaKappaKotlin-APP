package com.example.develarqapp.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.DownloadHistory
import com.example.develarqapp.data.repository.DownloadHistoryRepository
import kotlinx.coroutines.launch

class DownloadHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadHistoryRepository(application)

    private val _downloads = MutableLiveData<List<DownloadHistory>>()
    val downloads: LiveData<List<DownloadHistory>> = _downloads

    private val _filteredDownloads = MutableLiveData<List<DownloadHistory>>()
    val filteredDownloads: LiveData<List<DownloadHistory>> = _filteredDownloads

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    fun loadDownloadHistory() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getDownloadHistory()

            result.onSuccess { downloadList ->
                _downloads.value = downloadList
                _filteredDownloads.value = downloadList
                _isEmpty.value = downloadList.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al cargar historial"
                _downloads.value = emptyList()
                _isEmpty.value = true
            }

            _isLoading.value = false
        }
    }

    fun filterDownloads(
        userId: Long? = null,
        projectId: Long? = null,
        searchQuery: String = ""
    ) {
        val currentDownloads = _downloads.value ?: emptyList()

        val filtered = currentDownloads.filter { download ->
            val matchesUser = userId?.let { download.userId == it } ?: true
            val matchesProject = projectId?.let { download.proyectoId == it } ?: true
            val matchesSearch = if (searchQuery.isNotBlank()) {
                download.documentoNombre.contains(searchQuery, ignoreCase = true) ||
                        download.userName.contains(searchQuery, ignoreCase = true) ||
                        download.proyectoNombre?.contains(searchQuery, ignoreCase = true) == true
            } else true

            matchesUser && matchesProject && matchesSearch
        }

        _filteredDownloads.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun clearFilters() {
        _filteredDownloads.value = _downloads.value
        _isEmpty.value = _downloads.value?.isEmpty() ?: true
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}