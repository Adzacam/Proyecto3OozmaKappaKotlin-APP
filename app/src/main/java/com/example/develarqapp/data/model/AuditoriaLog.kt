package com.example.develarqapp.data.model

data class AuditoriaLog(
    val id: Long,
    val usuario: String,
    val accion: String,
    val registro: String,
    val fecha: String,
    val ip_address: String? = null
)