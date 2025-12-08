package com.example.develarqapp.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.develarqapp.R

/**
 * Helper para obtener colores dinámicos según el rol del usuario
 *
 * Ubicación: app/src/main/java/com/example/develarqapp/utils/RoleColorsHelper.kt
 */
object RoleColorsHelper {

    /**
     * Obtiene el color de acento principal según el rol
     */
    fun getAccentColor(context: Context, role: String): Int {
        return when (role.lowercase()) {
            "admin" -> ContextCompat.getColor(context, R.color.rol_admin_accent)
            "cliente" -> ContextCompat.getColor(context, R.color.rol_cliente_accent)
            "arquitecto" -> ContextCompat.getColor(context, R.color.rol_arquitecto_accent)
            "ingeniero" -> ContextCompat.getColor(context, R.color.rol_ingeniero_accent)
            else -> ContextCompat.getColor(context, R.color.primary_green)
        }
    }

    /**
     * Obtiene el color de acento hover según el rol
     */
    fun getAccentHoverColor(context: Context, role: String): Int {
        return when (role.lowercase()) {
            "admin" -> ContextCompat.getColor(context, R.color.rol_admin_accent_hover)
            "cliente" -> ContextCompat.getColor(context, R.color.rol_cliente_accent_hover)
            "arquitecto" -> ContextCompat.getColor(context, R.color.rol_arquitecto_accent_hover)
            "ingeniero" -> ContextCompat.getColor(context, R.color.rol_ingeniero_accent_hover)
            else -> ContextCompat.getColor(context, R.color.primary_green)
        }
    }

    /**
     * Obtiene el color de fondo de acento según el rol
     */
    fun getAccentBackgroundColor(context: Context, role: String): Int {
        return when (role.lowercase()) {
            "admin" -> ContextCompat.getColor(context, R.color.rol_admin_accent_bg)
            "cliente" -> ContextCompat.getColor(context, R.color.rol_cliente_accent_bg)
            "arquitecto" -> ContextCompat.getColor(context, R.color.rol_arquitecto_accent_bg)
            "ingeniero" -> ContextCompat.getColor(context, R.color.rol_ingeniero_accent_bg)
            else -> ContextCompat.getColor(context, R.color.background_slate)
        }
    }

    /**
     * Obtiene el color de texto muted según el rol
     */
    fun getTextMutedColor(context: Context, role: String): Int {
        return when (role.lowercase()) {
            "admin" -> ContextCompat.getColor(context, R.color.rol_admin_text_muted)
            "cliente" -> ContextCompat.getColor(context, R.color.rol_cliente_text_muted)
            "arquitecto" -> ContextCompat.getColor(context, R.color.rol_arquitecto_text_muted)
            "ingeniero" -> ContextCompat.getColor(context, R.color.rol_ingeniero_text_muted)
            else -> ContextCompat.getColor(context, R.color.text_gray)
        }
    }

    /**
     * Aplica el color de acento a una vista
     */
    fun applyAccentColorToView(context: Context, view: android.view.View, role: String) {
        view.setBackgroundColor(getAccentColor(context, role))
    }

    /**
     * Obtiene el nombre del rol capitalizado
     */
    fun getRoleDisplayName(role: String): String {
        return when (role.lowercase()) {
            "admin" -> "Administrador"
            "cliente" -> "Cliente"
            "arquitecto" -> "Arquitecto"
            "ingeniero" -> "Ingeniero"
            else -> role.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Obtiene un icono representativo del rol (si se necesita)
     */
    fun getRoleIcon(role: String): String {
        return when (role.lowercase()) {
            "admin" -> "👑"
            "cliente" -> "👤"
            "arquitecto" -> "📐"
            "ingeniero" -> "⚙️"
            else -> "👤"
        }
    }
}