package com.example.develarqapp.data.api

import com.example.develarqapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ========== AUTH ==========
    @POST("login/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("logout/")
    suspend fun logout(): Response<Void>

    @POST("forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    // ========== USERS ==========
    @GET("Users/get_users.php")
    suspend fun getUsers(): Response<UsersResponse>

    @POST("Users/create_user.php")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>

    @POST("Users/update_user.php")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @POST("Users/delete_user.php")
    suspend fun deleteUser(@Body request: DeleteUserRequest): Response<GenericResponse>

    @POST("Users/toggle_status.php")
    suspend fun toggleUserStatus(@Body request: DeleteUserRequest): Response<GenericResponse>
}