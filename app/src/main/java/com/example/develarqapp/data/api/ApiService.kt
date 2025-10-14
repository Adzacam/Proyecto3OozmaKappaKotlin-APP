package com.example.develarqapp.data.api


import com.example.develarqapp.data.model.LoginRequest
import com.example.develarqapp.data.model.LoginResponse

import com.example.develarqapp.data.model.ForgotPasswordRequest
import com.example.develarqapp.data.model.GenericResponse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// data/api/ApiService.kt

interface ApiService {
    @POST("login/") // Apunta a la carpeta /login/ que contiene index.php
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("logout/") // Apunta a la carpeta /logout/ que contiene index.php
    suspend fun logout(): Response<Void>

    @POST("forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>
}
