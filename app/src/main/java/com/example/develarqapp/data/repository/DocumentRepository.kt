package com.example.develarqapp.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import android.app.DownloadManager
import android.os.Environment
import android.util.Log
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import android.widget.Toast

class DocumentRepository(private val context: Context) {

    private val apiService = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)
    private val applicationContext = context.applicationContext
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
            // Validar según tipo
            if (tipo == DocumentType.URL) {
                if (enlaceExterno.isNullOrBlank()) {
                    return Result.failure(Exception("El enlace externo es requerido"))
                }
            } else {
                if (fileUri == null) {
                    return Result.failure(Exception("El archivo es requerido"))
                }
            }

            // Crear RequestBody para parámetros
            val projectIdBody = projectId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
            val descripcionBody = descripcion?.toRequestBody("text/plain".toMediaTypeOrNull())
            val tipoBody = tipo.name.toRequestBody("text/plain".toMediaTypeOrNull())
            val enlaceBody = enlaceExterno?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Preparar archivo si existe
            val filePart = if (fileUri != null) {
                prepareFilePart("archivo", fileUri)
            } else null

            // Hacer la petición
            val response = apiService.uploadDocument(
                projectIdBody,
                nombreBody,
                descripcionBody,
                tipoBody,
                filePart,
                enlaceBody,
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ACTUALIZAR DOCUMENTO ==========
    suspend fun updateDocument(
        id: Long,
        nombre: String,
        descripcion: String?,
        projectId: Long?,
        tipo: DocumentType?,
        enlaceExterno: String?
    ): Result<String> {
        return try {
            val request = UpdateDocumentRequest(
                id = id,
                nombre = nombre,
                descripcion = descripcion,
                proyectoId = projectId,
                tipo = tipo?.name,
                enlaceExterno = enlaceExterno
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ELIMINAR DOCUMENTO ==========
    suspend fun deleteDocument(documentId: Long): Result<String> {
        return try {
            val request = DeleteDocumentRequest(documentId)
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ========== ELIMINACIÓN PERMANENTE ==========

    suspend fun permanentDeleteDocument(documentId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Token no disponible"))
                }

                val request = DocumentIdRequest(id = documentId)
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
    // ========== REGISTRAR DESCARGAS ==========
    suspend fun registerDownload(documentId: Long): Result<String> {
        return try {
            val token = sessionManager.getToken()
                ?: return Result.failure(Exception("Token no disponible"))

            val request = RegisterDownloadRequest (documento_id = documentId)

            val response = apiService.registerDownload(request, "Bearer $token")

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Descarga registrada")
            } else {
                // No fallar si el registro falla, solo log
                Result.failure(Exception("No se pudo registrar la descarga"))
            }
        } catch (e: Exception) {
            // No interrumpir la descarga si falla el registro
            Result.failure(e)
        }
    }

    // PURGAR DOCUMENTOS ANTIGUOS (30+ días)
    suspend fun purgeOldDocuments(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Token no disponible"))
                }

                val response = apiService.purgeOldDocuments("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val purgeResponse = response.body()!!
                    if (purgeResponse.success) {
                        val deletedCount = purgeResponse.deletedCount ?: 0
                        val message = if (deletedCount > 0) {
                            "Se eliminaron $deletedCount documentos antiguos"
                        } else {
                            purgeResponse.message ?: "No hay documentos para purgar"
                        }
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
            val request = DocumentIdRequest(documentId)
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

    // ========== DESCARGAR DOCUMENTO ==========
    fun downloadDocument(documentId: Long, fileName: String): Result<String> {
        return try {
            val downloadUrl = "${ApiConfig.BASE_URL}Documents/download_document.php?id=$documentId"
            // 2. Configurar la solicitud de descarga
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                // Título y descripción de la notificación
                setTitle(fileName)
                setDescription("Descargando documento del proyecto...")

                // IMPORTANTE: Pasar el Token de Autorización en el Header
                addRequestHeader("Authorization", getAuthToken())

                // Configurar notificaciones (Visible durante y después)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Guardar en la carpeta pública de Descargas (Downloads)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                // Permitir que el sistema escanee el archivo (para que aparezca en la galería/apps)
                allowScanningByMediaScanner()
            }
            // 3. Enviar la solicitud al sistema
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Result.success("Descarga iniciada. Revisa tus notificaciones.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== UTILIDADES ==========

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"

            // Obtener nombre del archivo
            val fileName = getFileName(fileUri)

            // Crear archivo temporal
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

    private fun saveResponseBodyToFile(body: ResponseBody, fileName: String): File {
        val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)

        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

}