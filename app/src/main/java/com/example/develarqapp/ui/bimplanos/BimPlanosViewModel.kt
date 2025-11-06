package com.example.develarqapp.ui.bimplans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.BimPlanos
import com.example.develarqapp.data.repository.BimPlanosRepository
import kotlinx.coroutines.launch

class BimPlanosViewModel : ViewModel() {

    private val repository = BimPlanosRepository()

    private val _bimPlans = MutableLiveData<List<BimPlanos>>()
    val bimPlans: LiveData<List<BimPlanos>> = _bimPlans

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    fun loadBimPlans() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.getBimPlans()

                if (result.isSuccess) {
                    _bimPlans.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Error al cargar planos BIM"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadBimPlan(planId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.downloadBimPlan(planId)

                if (result.isSuccess) {
                    _successMessage.value = "Descarga iniciada"
                } else {
                    _errorMessage.value = "Error al descargar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteBimPlan(planId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.deleteBimPlan(planId)

                if (result.isSuccess) {
                    _successMessage.value = "Plano eliminado"
                    loadBimPlans() // Recargar lista
                } else {
                    _errorMessage.value = "Error al eliminar"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}