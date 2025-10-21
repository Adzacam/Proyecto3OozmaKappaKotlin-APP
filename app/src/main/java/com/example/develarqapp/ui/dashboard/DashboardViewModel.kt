package com.example.develarqapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.develarqapp.utils.SessionManager

class DashboardViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    fun loadUserData(sessionManager: SessionManager) {
        _userName.value = sessionManager.getUserName()
        _userRole.value = sessionManager.getUserRol()
    }

    fun setUserData(name: String, role: String) {
        _userName.value = name
        _userRole.value = role
    }
}