package com.example.develarqapp.ui.bimplans

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.data.model.PlanoVersion
import com.example.develarqapp.data.repository.BimPlanosRepository
import kotlinx.coroutines.launch

class BimPlanosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BimPlanosRepository(application)

    // ============================================
    // LIVEDATA - PLANOS ACTIVOS
    // ============================================
    private val _bimPlanos = MutableLiveData<List<BimPlano>>()
    val bimPlanos: LiveData<List<BimPlano>> = _bimPlanos

    // ============================================
    // LIVEDATA - PLANOS ELIMINADOS
    // ============================================
    private val _bimPlanosEliminados = MutableLiveData<List<BimPlano>>()
    val bimPlanosEliminados: LiveData<List<BimPlano>> = _bimPlanosEliminados

    // ============================================
    // LIVEDATA - ESTADOS
    // ============================================
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    fun resetOperationSuccess() {
        _operationSuccess.value = false
    }

    // ============================================
    // CARGAR PLANOS ACTIVOS
    // ============================================
    fun loadBimPlanos(
        projectId: Long? = null,
        tipo: String? = null,
        search: String? = null,
        fechaDesde: String? = null,
        fechaHasta: String? = null,
        orderBy: String = "fecha_subida",
        orderDir: String = "DESC"
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.getBimPlanos(
                    projectId = projectId,
                    tipo = tipo,
                    search = search,
                    fechaDesde = fechaDesde,
                    fechaHasta = fechaHasta,
                    orderBy = orderBy,
                    orderDir = orderDir
                )

                if (result.isSuccess) {
                    _bimPlanos.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error desconocido"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // CARGAR PAPELERA
    // ============================================
    fun loadDeletedBimPlanos() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.getDeletedBimPlanos()

                if (result.isSuccess) {
                    _bimPlanosEliminados.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar papelera"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // SUBIR NUEVO PLANO
    // ============================================
    fun uploadBimPlano(
        projectId: Long,
        nombre: String,
        descripcion: String?,
        tipo: String,
        fileUri: Uri?,
        enlaceExterno: String?
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.uploadBimPlano(
                    projectId = projectId,
                    nombre = nombre,
                    descripcion = descripcion,
                    tipo = tipo,
                    fileUri = fileUri,
                    enlaceExterno = enlaceExterno
                )

                if (result.isSuccess) {
                    _successMessage.value = "Plano subido correctamente"
                    _operationSuccess.value = true
                    loadBimPlanos() // Recargar lista
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al subir plano"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // ACTUALIZAR PLANO
    // ============================================
    fun updateBimPlano(
        id: Long,
        nombre: String,
        descripcion: String?
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.updateBimPlano(id, nombre, descripcion)

                if (result.isSuccess) {
                    _successMessage.value = "Plano actualizado correctamente"
                    _operationSuccess.value = true
                    loadBimPlanos() // Recargar lista
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

    // ============================================
    // ELIMINAR PLANO (PAPELERA)
    // ============================================
    fun deleteBimPlano(id: Long, motivo: String = "Sin motivo especificado") {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.deleteBimPlano(id, motivo)

                if (result.isSuccess) {
                    _successMessage.value = "Plano movido a papelera"
                    loadBimPlanos() // Recargar lista
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al eliminar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // RESTAURAR PLANO
    // ============================================
    fun restoreBimPlano(id: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.restoreBimPlano(id)

                if (result.isSuccess) {
                    _successMessage.value = "Plano restaurado correctamente"
                    loadDeletedBimPlanos() // Recargar papelera
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al restaurar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // ELIMINAR PERMANENTEMENTE
    // ============================================
    fun permanentDeleteBimPlano(id: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.permanentDeleteBimPlano(id)

                if (result.isSuccess) {
                    _successMessage.value = "Plano eliminado permanentemente"
                    loadDeletedBimPlanos() // Recargar papelera
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al eliminar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // ============================================
    // LIVEDATA - VERSIONES
    // ============================================
        private val _planoVersions = MutableLiveData<List<PlanoVersion>>()
        val planoVersions: LiveData<List<PlanoVersion>> = _planoVersions

        // ============================================
        // CARGAR HISTORIAL DE VERSIONES
        // ============================================
            fun loadPlanoVersions(planoId: Long) {
                _isLoading.value = true

                viewModelScope.launch {
                    try {
                        val result = repository.getPlanoVersions(planoId)

                        if (result.isSuccess) {
                            _planoVersions.value = result.getOrNull() ?: emptyList()
                        } else {
                            _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar versiones"
                        }
                    } catch (e: Exception) {
                        _errorMessage.value = "Error: ${e.message}"
                    } finally {
                        _isLoading.value = false
                    }
                }
            }

    // ============================================
    // ESTABLECER VERSIÓN COMO ACTUAL
    // ============================================
        fun setVersionActual(versionId: Long, planoId: Long) {
            _isLoading.value = true

            viewModelScope.launch {
                try {
                    val result = repository.setVersionActual(versionId)

                    if (result.isSuccess) {
                        _successMessage.value = "Versión actualizada"
                        loadPlanoVersions(planoId) // Recargar historial
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Error"
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

    // ============================================
    // SUBIR NUEVA VERSIÓN
    // ============================================
        fun uploadNewVersion(
            planoOriginalId: Long,
            nombre: String,
            descripcion: String?,
            tipo: String,
            fileUri: Uri?
        ) {
            _isLoading.value = true

            viewModelScope.launch {
                try {
                    val result = repository.uploadNewVersion(
                        planoOriginalId = planoOriginalId,
                        nombre = nombre,
                        descripcion = descripcion,
                        tipo = tipo,
                        fileUri = fileUri
                    )

                    if (result.isSuccess) {
                        _successMessage.value = "Nueva versión subida correctamente"
                        _operationSuccess.value = true
                        loadBimPlanos() // Recargar lista principal
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al subir versión"
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

    // ============================================
    // HELPERS
    // ============================================
    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}