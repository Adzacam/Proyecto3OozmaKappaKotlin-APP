package com.example.develarqapp.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.AuditManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(private val context: Context) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)

    // Obtener token con Bearer
    private fun getAuthToken(): String {
        return "Bearer ${sessionManager.getToken()}"
    }

    // ========== OBTENER DOCUMENTOS ==========
    suspend fun getDocuments(projectId: Long? = null): Result<List<Document>> {
        return try {
            val response = apiService.getDocuments(getAuthToken(), projectId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error desconocido"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== OBTENER DOCUMENTOS ELIMINADOS ==========
    suspend fun getDeletedDocuments(): Result<List<Document>> {
        return try {
            val response = apiService.getDeletedDocuments(getAuthToken())

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error desconocido"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== SUBIR DOCUMENTO ==========
    suspend fun uploadDocument(
        projectId: Long,
        nombre: String,
        descripcion: String?,
        tipo: DocumentType,
        fileUri: Uri?,
        enlaceExterno: String?
    ): Result<Document> {
        return try {
            // Validar
            if (tipo == DocumentType.URL && enlaceExterno.isNullOrBlank()) {
                return Result.failure(Exception("El enlace externo es requerido"))
            } else if (tipo != DocumentType.URL && fileUri == null) {
                return Result.failure(Exception("El archivo es requerido"))
            }

            // RequestBodies
            val mediaType = "text/plain".toMediaTypeOrNull()
            val projectIdBody = projectId.toString().toRequestBody(mediaType)
            val nombreBody = nombre.toRequestBody(mediaType)
            val descripcionBody = descripcion?.toRequestBody(mediaType)
            val tipoBody = tipo.name.toRequestBody(mediaType)
            val enlaceBody = enlaceExterno?.toRequestBody(mediaType)

            // Auditoría
            val deviceInfo = AuditManager.getDeviceInfo()
            val deviceModelBody = (deviceInfo["device_model"] as String).toRequestBody(mediaType)
            val androidVerBody = (deviceInfo["android_version"] as String).toRequestBody(mediaType)
            val sdkVerBody = (deviceInfo["sdk_version"] as Int).toString().toRequestBody(mediaType)

            val filePart = if (fileUri != null) prepareFilePart("archivo", fileUri) else null

            val response = apiService.uploadDocument(
                projectIdBody, nombreBody, descripcionBody, tipoBody,
                filePart, enlaceBody,
                deviceModelBody, androidVerBody, sdkVerBody,
                getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "Error al subir documento"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // ========== ACTUALIZAR DOCUMENTO ==========
    suspend fun updateDocument(
        id: Long, nombre: String, descripcion: String?, projectId: Long?, tipo: DocumentType?, enlaceExterno: String?
    ): Result<String> {
        return try {
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = UpdateDocumentRequest(
                id = id, nombre = nombre, descripcion = descripcion,
                proyectoId = projectId, tipo = tipo?.name, enlaceExterno = enlaceExterno,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = apiService.updateDocument(request, getAuthToken())
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.message ?: "Documento actualizado")
                } else {
                    Result.failure(Exception(body?.message ?: "Error al actualizar"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // ========== ELIMINAR DOCUMENTO (PAPELERA) ==========
    suspend fun deleteDocument(documentId: Long): Result<String> {
        return try {
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = DeleteDocumentRequest(
                id = documentId,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = apiService.deleteDocument(request, getAuthToken())

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.message ?: "Documento eliminado")
                } else {
                    Result.failure(Exception(body?.message ?: "Error al eliminar"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // ========== ELIMINACIÓN PERMANENTE ==========
    suspend fun permanentDeleteDocument(documentId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Token no disponible"))
                val deviceInfo = AuditManager.getDeviceInfo()

                val request = DocumentIdRequest(
                    id = documentId,
                    deviceModel = deviceInfo["device_model"] as String,
                    androidVersion = deviceInfo["android_version"] as String,
                    sdkVersion = deviceInfo["sdk_version"] as Int
                )

                val response = apiService.permanentDeleteDocument(request, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (genericResponse.success) {
                        Result.success(genericResponse.message ?: "Documento eliminado permanentemente")
                    } else {
                        Result.failure(Exception(genericResponse.message ?: "Error desconocido"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("DocumentRepository", "Error al eliminar permanentemente: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // ========== PURGAR DOCUMENTOS ANTIGUOS ==========
    suspend fun purgeOldDocuments(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Token no disponible"))
                val deviceInfo = AuditManager.getDeviceInfo()

                val request = PurgeRequest(
                    deviceModel = deviceInfo["device_model"] as String,
                    androidVersion = deviceInfo["android_version"] as String,
                    sdkVersion = deviceInfo["sdk_version"] as Int
                )
                val response = apiService.purgeOldDocuments(request, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val purgeResponse = response.body()!!
                    // Aquí se corrigen las referencias a 'deletedCount' y 'success'
                    if (purgeResponse.success) {
                        val count = purgeResponse.deletedCount ?: 0
                        val message = if (count > 0) "Se eliminaron $count documentos" else purgeResponse.message ?: "Nada para purgar"
                        Result.success(message)
                    } else {
                        Result.failure(Exception(purgeResponse.message ?: "Error desconocido"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("DocumentRepository", "Error al purgar documentos: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // ========== RESTAURAR DOCUMENTO ==========
    suspend fun restoreDocument(documentId: Long): Result<String> {
        return try {
            val deviceInfo = AuditManager.getDeviceInfo()
            val request = DocumentIdRequest(
                id = documentId,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )
            val response = apiService.restoreDocument(request, getAuthToken())

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.message ?: "Documento restaurado")
                } else {
                    Result.failure(Exception(body?.message ?: "Error al restaurar"))
                }
            } else {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== REGISTRAR DESCARGAS ==========
    suspend fun registerDownload(documentId: Long): Result<String> {
        return try {
            val token = sessionManager.getToken() ?: return Result.failure(Exception("Token no disponible"))
            val request = RegisterDownloadRequest(documento_id = documentId)
            val response = apiService.registerDownload(request, "Bearer $token")

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Descarga registrada")
            } else {
                Result.failure(Exception("No se pudo registrar la descarga"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== DESCARGAR DOCUMENTO ==========
    fun downloadDocument(documentId: Long, fileName: String): Result<String> {
        return try {
            val downloadUrl = "${ApiConfig.BASE_URL}Documents/download_document.php?id=$documentId"

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(fileName)
                setDescription("Descargando documento...")
                addRequestHeader("Authorization", getAuthToken())
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Result.success("Descarga iniciada. Revisa tus notificaciones.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== UTILIDADES INTERNAS ==========

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
            val fileName = getFileName(fileUri)

            val inputStream = contentResolver.openInputStream(fileUri)
            val tempFile = File(context.cacheDir, fileName)

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, fileName, requestFile)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "documento_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}