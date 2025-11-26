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

    @GET("Users/get_deleted_users.php")
    suspend fun getDeletedUsers(): Response<UsersResponse>

    @POST("Users/create_user.php")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>

    @POST("Users/update_user.php")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @POST("Users/delete_user.php")
    suspend fun deleteUser(@Body request: DeleteUserRequest): Response<GenericResponse>

    @POST("Users/restore_user.php")
    suspend fun restoreUser(@Body request: DeleteUserRequest): Response<GenericResponse>

    @POST("Users/toggle_status.php")
    suspend fun toggleUserStatus(@Body request: DeleteUserRequest): Response<GenericResponse>

    @POST("Users/update_password.php")
    suspend fun updatePassword(
        @Body request: UpdatePasswordRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Users/update_user.php")
    suspend fun updateUser(
        @Body request: UpdateUserRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== PROYECTOS ==========
    
    /**
     * Obtener lista de proyectos (filtrado por permisos en backend)
     */
    @GET("Proyectos/get_projects.php")
    suspend fun getProjects(): Response<ApiResponse<List<Project>>>

    /**
     * Cambiar estado de un proyecto
     */
    @POST("Proyectos/cambiar_estado.php")
    suspend fun changeProjectState(
        @Body request: ChangeProjectStateRequest,
        @Query("id") projectId: Long
    ): Response<ApiResponse<Any>>
    
}