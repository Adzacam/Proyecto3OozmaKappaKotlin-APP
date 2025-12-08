package com.example.develarqapp.utils

import android.os.Build

object DeviceInfoUtil {

    /**
     * Obtiene el modelo del dispositivo
     * Ejemplo: "Samsung SM-G973F", "Google Pixel 5"
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.capitalize()
        } else {
            "${manufacturer.capitalize()} $model"
        }
    }

    /**
     * Obtiene la versión de Android en formato legible
     * Ejemplo: "Android 13", "Android 11"
     */
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    /**
     * Obtiene el nivel de API (SDK)
     * Ejemplo: 33, 30, 29
     */
    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * Obtiene información completa del dispositivo en formato String
     */
    fun getDeviceInfo(): String {
        return buildString {
            appendLine("Modelo: ${getDeviceModel()}")
            appendLine("Android: ${getAndroidVersion()}")
            appendLine("SDK: ${getSdkVersion()}")
            appendLine("Marca: ${Build.BRAND.capitalize()}")
            appendLine("Producto: ${Build.PRODUCT}")
        }
    }

    /**
     * Capitaliza la primera letra de un string
     */
    private fun String.capitalize(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}