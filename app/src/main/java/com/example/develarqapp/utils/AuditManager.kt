package com.example.develarqapp.utils

import android.os.Build
import com.google.gson.annotations.SerializedName

/**
 * 🎯 Gestor centralizado de auditoría
 *
 * Proporciona métodos para preparar información de auditoría
 * que se envía al backend con cada operación crítica.
 */
object AuditManager {

    /**
     * 📦 Crea un mapa con información del dispositivo
     *
     * @return Map con device_model, android_version y sdk_version
     */
    fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "device_model" to getDeviceModel(),
            "android_version" to getAndroidVersion(),
            "sdk_version" to Build.VERSION.SDK_INT
        )
    }

    /**
     * 📱 Obtiene el modelo del dispositivo
     */
    private fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.capitalize()
        } else {
            "${manufacturer.capitalize()} $model"
        }
    }

    /**
     * 🤖 Obtiene la versión de Android
     */
    private fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    /**
     * 🔧 Capitaliza la primera letra
     */
    private fun String.capitalize(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    /**
     * 📝 Crea un request de eliminación con motivo
     *
     * Útil para operaciones que requieren justificación
     */
    fun createDeleteRequest(
        id: Long,
        motivo: String
    ): DeleteWithReasonRequest {
        val deviceInfo = getDeviceInfo()
        return DeleteWithReasonRequest(
            id = id,
            motivo = motivo,
            deviceModel = deviceInfo["device_model"] as String,
            androidVersion = deviceInfo["android_version"] as String,
            sdkVersion = deviceInfo["sdk_version"] as Int
        )
    }

    /**
     * 🔄 Añade información del dispositivo a un request existente
     *
     * @param baseRequest Request original (debe ser mutable)
     * @return Map con el request más la info del dispositivo
     */
    fun enrichWithDeviceInfo(baseRequest: Map<String, Any>): Map<String, Any> {
        return baseRequest.toMutableMap().apply {
            putAll(getDeviceInfo())
        }
    }
}

/**
 * 📋 Request modelo para eliminaciones con motivo
 */
data class DeleteWithReasonRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("motivo")
    val motivo: String,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("android_version")
    val androidVersion: String,

    @SerializedName("sdk_version")
    val sdkVersion: Int
)