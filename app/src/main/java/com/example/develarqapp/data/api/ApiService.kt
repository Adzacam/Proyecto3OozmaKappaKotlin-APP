package com.example.develarqapp.data.api

import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.model.UsersResponse
import com.example.develarqapp.data.repository.CalendarRepository
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody

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
    suspend fun createUser(
        @Body request: CreateUserRequest,
        @Header("Authorization") token: String
    ): Response<UserResponse>

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

    @GET("Audit/get_audit_logs.php")
    suspend fun getAuditLogs(
        @Header("Authorization") token: String
    ): Response<List<AuditoriaLog>>

    // ========== PROJECTS ==========
    // (Basado en tu captura de pantalla de VSCode)
    @GET("Projects/get_Projects.php")
    suspend fun getProjects(@Header("Authorization") token: String): Response<ProjectsResponse>

    // ========== CALENDAR ==========

    @GET("Calendar/get_Meetings.php")
    suspend fun getMeetings(@Header("Authorization") token: String): Response<MeetingsResponse>

    @GET("Calendar/get_Meetings.php")
    suspend fun getMeetingsByProject(
        @Query("proyecto_id") projectId: Long,
        @Header("Authorization") token: String
    ): Response<MeetingsResponse>

    @POST("Calendar/create_Meetings.php")
    suspend fun createMeeting(
        @Body meeting: MeetingRequest,
        @Header("Authorization") token: String
    ): Response<MeetingResponse>

    @POST("Calendar/update_Meeting.php")
    suspend fun updateMeeting(
        @Query("id") meetingId: Long,
        @Body meeting: MeetingRequest,
        @Header("Authorization") token: String
    ): Response<MeetingResponse>

    @POST("Calendar/delete_Meetings.php")
    suspend fun deleteMeeting(
        @Query("id") meetingId: Long,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== DOCUMENTS ==========
    @GET("Documents/get_documents.php")
    suspend fun getDocuments(
        @Header("Authorization") token: String,
        @Query("proyecto_id") projectId: Long? = null
    ): Response<DocumentsResponse>

    @GET("Documents/get_deleted_documents.php")
    suspend fun getDeletedDocuments(
        @Header("Authorization") token: String
    ): Response<DocumentsResponse>

    @Multipart
    @POST("Documents/upload_document.php")
    suspend fun uploadDocument(
        @Part("proyecto_id") projectId: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("descripcion") descripcion: RequestBody?,
        @Part("tipo") tipo: RequestBody,
        @Part archivo: MultipartBody.Part?,
        @Part("enlace_externo") enlaceExterno: RequestBody?,
        @Header("Authorization") token: String
    ): Response<DocumentResponse>

    @POST("Documents/update_document.php")
    suspend fun updateDocument(
        @Body request: UpdateDocumentRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Documents/delete_document.php")
    suspend fun deleteDocument(
        @Body request: DeleteDocumentRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Documents/restore_document.php")
    suspend fun restoreDocument(
        @Body request: DocumentIdRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @GET("Documents/download_document.php")
    @Streaming
    suspend fun downloadDocument(
        @Query("id") documentId: Long,
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}