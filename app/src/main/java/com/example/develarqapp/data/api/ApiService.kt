package com.example.develarqapp.data.api

import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.model.UsersResponse
import com.example.develarqapp.data.repository.CalendarRepository
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
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): Response<UsersResponse>

    @GET("Users/get_deleted_users.php")
    suspend fun getDeletedUsers(
        @Header("Authorization") token: String
    ): Response<UsersResponse>

    @POST("Users/create_user.php")
    suspend fun createUser( // <-- Función que faltaba
        @Body request: CreateUserRequest,
        @Header("Authorization") token: String
    ): Response<UserResponse> // (Asumo que devuelve UserResponse)

    @POST("Users/delete_user.php")
    suspend fun deleteUser(
        @Body request: DeleteUserRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Users/restore_user.php")
    suspend fun restoreUser(
        @Body request: DeleteUserRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Users/toggle_status.php")
    suspend fun toggleUserStatus(
        @Body request: DeleteUserRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

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

    // ========== AUDITORIA ==========

    @GET("Auditoria/get_audit_logs.php")
    suspend fun getAuditLogs(
        @Header("Authorization") token: String
    ): Response<List<AuditoriaLog>>

    // ========== PROJECTS ==========
    // (Basado en tu captura de pantalla de VSCode)
    @GET("Calendar/get_proyectos.php")
    suspend fun getProjects(@Header("Authorization") token: String): Response<ProjectsResponse>

    // ========== CALENDAR ==========

    @GET("Calendar/get_reuniones.php")
    suspend fun getMeetings(@Header("Authorization") token: String): Response<MeetingsResponse>

    @GET("Calendar/get_reuniones.php")
    suspend fun getMeetingsByProject(
        @Query("proyecto_id") projectId: Long,
        @Header("Authorization") token: String
    ): Response<MeetingsResponse>

    @POST("Calendar/create_reunion.php") // Asumo que este es el nombre de tu archivo PHP
    suspend fun createMeeting(
        @Body meeting: MeetingRequest,
        @Header("Authorization") token: String
    ): Response<MeetingResponse>

    @POST("Calendar/update_reunion.php")
    suspend fun updateMeeting(
        @Query("id") meetingId: Long,
        @Body meeting: MeetingRequest,
        @Header("Authorization") token: String
    ): Response<MeetingResponse>

    @POST("Calendar/delete_reunion.php")
    suspend fun deleteMeeting(
        @Query("id") meetingId: Long,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

}