package com.example.develarqapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.AuditManager
import com.example.develarqapp.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class BimPlanosRepository(private val context: Context) {

    private val api = ApiConfig.getApiService()
    private val sessionManager = SessionManager(context)

    // ============================================
    // OBTENER PLANOS BIM
    // ============================================
    suspend fun getBimPlanos(
        projectId: Long? = null,
        tipo: String? = null,
        search: String? = null,
        fechaDesde: String? = null,
        fechaHasta: String? = null,
        orderBy: String = "fecha_subida",
        orderDir: String = "DESC"
    ): Result<List<BimPlano>> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"

            val response = api.getBimPlanos(
                token = token,
                projectId = projectId,
                tipo = tipo,
                search = search,
                fechaDesde = fechaDesde,
                fechaHasta = fechaHasta,
                orderBy = orderBy,
                orderDir = orderDir
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar planos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // OBTENER PLANOS ELIMINADOS
    // ============================================
    suspend fun getDeletedBimPlanos(): Result<List<BimPlano>> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val response = api.getDeletedBimPlanos(token)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar papelera"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // SUBIR PLANO BIM
    // ============================================
    suspend fun uploadBimPlano(
        projectId: Long,
        nombre: String,
        descripcion: String?,
        tipo: String,
        fileUri: Uri?,
        enlaceExterno: String?
    ): Result<BimPlano> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            // Convertir parámetros básicos a RequestBody
            val projectIdBody = projectId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
            val descripcionBody = descripcion?.toRequestBody("text/plain".toMediaTypeOrNull())
            val tipoBody = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
            val enlaceBody = enlaceExterno?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Convertir device info a RequestBody
            val deviceModelBody = (deviceInfo["device_model"] as String).toRequestBody("text/plain".toMediaTypeOrNull())
            val androidVersionBody = (deviceInfo["android_version"] as String).toRequestBody("text/plain".toMediaTypeOrNull())
            val sdkVersionBody = (deviceInfo["sdk_version"] as Int).toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // Preparar archivo si existe
            var filePart: MultipartBody.Part? = null
            if (fileUri != null) {
                val file = uriToFile(fileUri)
                val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                filePart = MultipartBody.Part.createFormData("archivo", file.name, requestFile)
            }

            val response = api.uploadBimPlano(
                projectId = projectIdBody,
                nombre = nombreBody,
                descripcion = descripcionBody,
                tipo = tipoBody,
                archivo = filePart,
                enlaceExterno = enlaceBody,
                deviceModel = deviceModelBody,
                androidVersion = androidVersionBody,
                sdkVersion = sdkVersionBody,
                token = token
            )

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No se recibió información del plano"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al subir plano"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // ACTUALIZAR PLANO
    // ============================================
    suspend fun updateBimPlano(
        id: Long,
        nombre: String,
        descripcion: String?
    ): Result<Unit> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = UpdateBimPlanoRequest(
                id = id,
                nombre = nombre,
                descripcion = descripcion,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = api.updateBimPlano(request, token)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al actualizar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // ELIMINAR PLANO (MOVER A PAPELERA)
    // ============================================
    suspend fun deleteBimPlano(id: Long, motivo: String): Result<Unit> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = DeleteBimPlanoRequest(
                id = id,
                motivo = motivo,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = api.deleteBimPlano(request, token)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al eliminar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // RESTAURAR PLANO
    // ============================================
    suspend fun restoreBimPlano(id: Long): Result<Unit> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = BimPlanoIdRequest(
                id = id,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = api.restoreBimPlano(request, token)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al restaurar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // ELIMINAR PERMANENTEMENTE
    // ============================================
    suspend fun permanentDeleteBimPlano(id: Long): Result<Unit> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            val request = BimPlanoIdRequest(
                id = id,
                deviceModel = deviceInfo["device_model"] as String,
                androidVersion = deviceInfo["android_version"] as String,
                sdkVersion = deviceInfo["sdk_version"] as Int
            )

            val response = api.permanentDeleteBimPlano(request, token)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al eliminar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ============================================
    // OBTENER HISTORIAL DE VERSIONES
    // ============================================
        suspend fun getPlanoVersions(planoId: Long): Result<List<PlanoVersion>> {
            return try {
                val token = "Bearer ${sessionManager.getAuthToken()}"
                val response = api.getPlanoVersions(planoId, token)

                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Error al cargar versiones"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ============================================
    // ESTABLECER VERSIÓN COMO ACTUAL
    // ============================================
        suspend fun setVersionActual(versionId: Long): Result<Unit> {
            return try {
                val token = "Bearer ${sessionManager.getAuthToken()}"
                val deviceInfo = AuditManager.getDeviceInfo()

                val request = SetVersionActualRequest(
                    versionId = versionId,
                    deviceModel = deviceInfo["device_model"] as String,
                    androidVersion = deviceInfo["android_version"] as String,
                    sdkVersion = deviceInfo["sdk_version"] as Int
                )

                val response = api.setVersionActual(request, token)

                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ============================================
    // SUBIR NUEVA VERSIÓN DE PLANO EXISTENTE
    // ============================================
    suspend fun uploadNewVersion(
        planoOriginalId: Long,
        nombre: String,
        descripcion: String?,
        tipo: String,
        fileUri: Uri?
    ): Result<BimPlano> {
        return try {
            val token = "Bearer ${sessionManager.getAuthToken()}"
            val deviceInfo = AuditManager.getDeviceInfo()

            android.util.Log.d("BimPlanosRepo", "✅ Subiendo nueva versión: nombre='$nombre', tipo='$tipo'")

            val planoResult = getBimPlanos()
            val planoOriginal = planoResult.getOrNull()?.find { it.id == planoOriginalId }

            if (planoOriginal == null) {
                android.util.Log.e("BimPlanosRepo", "❌ Plano ID $planoOriginalId no encontrado")
                return Result.failure(Exception("Plano original no encontrado"))
            }

            android.util.Log.d("BimPlanosRepo", "✅ Proyecto ID: ${planoOriginal.proyectoId}")

            val projectIdBody = planoOriginal.proyectoId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
            val descripcionBody = descripcion?.toRequestBody("text/plain".toMediaTypeOrNull())
            val tipoBody = tipo.toRequestBody("text/plain".toMediaTypeOrNull())

            val deviceModelBody = (deviceInfo["device_model"] as String).toRequestBody("text/plain".toMediaTypeOrNull())
            val androidVersionBody = (deviceInfo["android_version"] as String).toRequestBody("text/plain".toMediaTypeOrNull())
            val sdkVersionBody = (deviceInfo["sdk_version"] as Int).toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // Preparar archivo
            var filePart: MultipartBody.Part? = null
            if (fileUri != null) {
                val file = uriToFile(fileUri)
                val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                filePart = MultipartBody.Part.createFormData("archivo", file.name, requestFile)
            }

            val response = api.uploadBimPlano(
                projectId = projectIdBody,
                nombre = nombreBody,
                descripcion = descripcionBody,
                tipo = tipoBody,
                archivo = filePart,
                enlaceExterno = null,
                deviceModel = deviceModelBody,
                androidVersion = androidVersionBody,
                sdkVersion = sdkVersionBody,
                token = token
            )

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let {
                    android.util.Log.d("BimPlanosRepo", "✅ Nueva versión creada: ID ${it.id}, versión ${it.version}")
                    Result.success(it)
                } ?: Result.failure(Exception("No se recibió información del plano"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al subir versión"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BimPlanosRepo", "❌ Error: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // HELPER: CONVERTIR URI A FILE
    // ============================================
    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")

        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}