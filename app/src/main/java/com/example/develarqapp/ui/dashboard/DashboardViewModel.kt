package com.example.develarqapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    init {
        // Valores por defecto
        _userName.value = "Adriano Leandro"
        _userRole.value = "ingeniero"
    }

    fun setUserData(name: String, role: String) {
        _userName.value = name
        _userRole.value = role
    }
}
