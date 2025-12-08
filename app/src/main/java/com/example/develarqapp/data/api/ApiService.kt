package com.example.develarqapp.data.api

import com.example.develarqapp.data.model.*
import com.example.develarqapp.data.model.UsersResponse
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
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<GenericResponse>

    @POST("forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    // ========== AUDITORIA ==========

    @GET("Audit/get_audit_logs.php")
    suspend fun getAuditLogs(
        @Header("Authorization") token: String
    ): Response<List<AuditoriaLog>>

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
    ): Response<DeleteUserResponse>

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

    // ========== PROJECTS ==========
    @GET("Projects/get_Projects.php")
    suspend fun getProjects(@Header("Authorization") token: String): Response<ProjectsResponse>

    // Crear Proyecto
    @POST("Projects/create_project.php")
    suspend fun createProject(
        @Body request: CreateProjectRequest, // Asegúrate de tener este Data Class
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // Actualizar Proyecto
    @POST("Projects/update_project.php")
    suspend fun updateProject(
        @Body request: UpdateProjectRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    //Actualizar Permisos
    @POST("Projects/update_permissions.php")
    suspend fun updateProjectPermissions(
        @Body request: UpdatePermissionsRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== HITOS (MILESTONES) - SECCIÓN NUEVA ==========

    @GET("Projects/get_project_hitos.php")
    suspend fun getHitos(
        @Query("proyecto_id") projectId: Long,
        @Header("Authorization") token: String
    ): Response<HitosResponse>

    @POST("Projects/create_hito.php")
    suspend fun createHito(
        @Body request: CreateHitoRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Projects/update_hito.php")
    suspend fun updateHito(
        @Body request: UpdateHitoRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Projects/delete_hito.php")
    suspend fun deleteHito(
        @Body request: DeleteHitoRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== PERMISOS Y TIMELINE ==========

    @GET("Projects/get_permissions.php")
    suspend fun getProjectPermissions( // Asegúrate que el nombre coincida con lo que llama el Repository
        @Query("proyecto_id") projectId: Long,
        @Header("Authorization") token: String
    ): Response<PermissionsResponse> // Necesitas crear PermissionsResponse si no existe

    @GET("Projects/get_timeline.php")
    suspend fun getTimeline(
        @Query("proyecto_id") projectId: Long,
        @Header("Authorization") token: String
    ): Response<TimelineResponse>

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

    @HTTP(method = "DELETE", path = "Meetings/delete_Meetings.php", hasBody = true)
    fun deleteMeeting(
        @Query("id") meetingId: Long,
        @Body deviceInfo: Map<String, Any>,
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
        @Part("device_model") deviceModel: RequestBody,
        @Part("android_version") androidVersion: RequestBody,
        @Part("sdk_version") sdkVersion: RequestBody,
        @Header("Authorization") token: String
    ): Response<DocumentResponse>

    @POST("Documents/purge_old_documents.php")
    suspend fun purgeOldDocuments(
        @Body request: PurgeRequest,
        @Header("Authorization") token: String
    ): Response<PurgeResponse>

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

    @POST("Documents/permanent_delete_document.php")
    suspend fun permanentDeleteDocument(
        @Body request: DocumentIdRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== DOWNLOAD HISTORY ==========
    @GET("Documents/get_download_history.php")
    suspend fun getDownloadHistory(
        @Header("Authorization") token: String
    ): Response<DownloadHistoryResponse>

    @GET("Documents/get_download_history.php")
    suspend fun getDownloadHistoryFiltered(
        @Query("user_id") userId: Long? = null,
        @Query("proyecto_id") projectId: Long? = null,
        @Query("fecha_inicio") startDate: String? = null,
        @Query("fecha_fin") endDate: String? = null,
        @Header("Authorization") token: String
    ): Response<DownloadHistoryResponse>

    // ========== REGISTRAR DESCARGA EN HISTORIAL ==========

    @POST("Documents/register_download.php")
    suspend fun registerDownload(
        @Body request: RegisterDownloadRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== BIM PLANOS ==========
    @GET("BimPlanos/get_bim_planos.php")
    suspend fun getBimPlanos(
        @Header("Authorization") token: String,
        @Query("proyecto_id") projectId: Long? = null,
        @Query("tipo") tipo: String? = null,
        @Query("search") search: String? = null,
        @Query("fecha_desde") fechaDesde: String? = null,
        @Query("fecha_hasta") fechaHasta: String? = null,
        @Query("order_by") orderBy: String? = "fecha_subida",
        @Query("order_dir") orderDir: String? = "DESC"
    ): Response<BimPlanosResponse>

    @GET("BimPlanos/get_deleted_bim_planos.php")
    suspend fun getDeletedBimPlanos(
        @Header("Authorization") token: String
    ): Response<BimPlanosResponse>

    @Multipart
    @POST("BimPlanos/upload_bim_plano.php")
    suspend fun uploadBimPlano(
        @Part("proyecto_id") projectId: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("descripcion") descripcion: RequestBody?,
        @Part("tipo") tipo: RequestBody,
        @Part archivo: MultipartBody.Part?,
        @Part("enlace_externo") enlaceExterno: RequestBody?,
        @Part("device_model") deviceModel: RequestBody,
        @Part("android_version") androidVersion: RequestBody,
        @Part("sdk_version") sdkVersion: RequestBody,
        @Header("Authorization") token: String
    ): Response<BimPlanoResponse>

    @POST("BimPlanos/update_bim_plano.php")
    suspend fun updateBimPlano(
        @Body request: UpdateBimPlanoRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("BimPlanos/delete_bim_plano.php")
    suspend fun deleteBimPlano(
        @Body request: DeleteBimPlanoRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("BimPlanos/restore_bim_plano.php")
    suspend fun restoreBimPlano(
        @Body request: BimPlanoIdRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("BimPlanos/permanent_delete_bim_plano.php")
    suspend fun permanentDeleteBimPlano(
        @Body request: BimPlanoIdRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== BIM PLANOS - VERSIONES ==========
    @GET("BimPlanos/get_plano_versions.php")
    suspend fun getPlanoVersions(
        @Query("plano_id") planoId: Long,
        @Header("Authorization") token: String
    ): Response<PlanoVersionsResponse>

    @POST("BimPlanos/set_version_actual.php")
    suspend fun setVersionActual(
        @Body request: SetVersionActualRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== NOTIFICATIONS ==========
    @GET("Notifications/get_notifications.php")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("tipo") tipo: String? = null,
        @Query("leida") leida: Int? = null,
        @Query("limit") limit: Int? = 50
    ): Response<NotificationsResponse>

    @POST("Notifications/mark_notification_read.php")
    suspend fun markNotificationRead(
        @Body request: MarkNotificationReadRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Notifications/mark_all_notifications_read.php")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // ========== DASHBOARD ==========
    @GET("Dashboard/get_dashboard_stats.php")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): Response<DashboardStatsResponse>

    // ========== TASKS (KANBAN) ==========
    @GET("Tasks/get_task.php")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Query("proyecto_id") projectId: Long? = null,
        @Query("asignado_id") asignadoId: Long? = null,
        @Query("estado") estado: String? = null
    ): Response<TasksResponse>

    @POST("Tasks/create_task.php")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
        @Header("Authorization") token: String
    ): Response<TaskResponse>

    @POST("Tasks/update_task.php")
    suspend fun updateTask(
        @Body request: UpdateTaskRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Tasks/update_task_state.php")
    suspend fun updateTaskState(
        @Body request: UpdateTaskStateRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    @POST("Tasks/delete_task.php")
    suspend fun deleteTask(
        @Body request: DeleteTaskRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>
}