package com.example.develarqapp.utils

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.LogoutRequest
import kotlinx.coroutines.launch
import com.example.develarqapp.utils.RoleColorsHelper


/**
 * Clase utilitaria para configurar el TopBar de forma consistente en todos los fragments
 */
class TopBarManager(
    private val fragment: Fragment,
    private val sessionManager: SessionManager
) {

    private val apiService by lazy { ApiConfig.getApiService() }

    /**
     * Configura el TopBar completo usando el layout incluido
     * @param topBarView La vista raíz del topAppBar incluido
     */
    fun setupTopBar(topBarView: View) {
        val menuIcon = topBarView.findViewById<ImageView>(R.id.ivMenuIcon)
        val notificationIcon = topBarView.findViewById<ImageView>(R.id.ivNotificationIcon)
        val userProfileLayout = topBarView.findViewById<LinearLayout>(R.id.llUserProfile)
        val userNameTextView = topBarView.findViewById<TextView>(R.id.tvUserName)

        // Configurar menú hamburguesa
        menuIcon?.setOnClickListener {
            (fragment.activity as? MainActivity)?.openDrawer()
        }

        // Configurar nombre de usuario y aplicar color del rol
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        val userRole = sessionManager.getUserRol()

        userNameTextView?.text = "$userName $userApellido"

        // Aplicar color del rol al nombre del usuario
        val accentColor = RoleColorsHelper.getAccentColor(fragment.requireContext(), userRole)
        userNameTextView?.setTextColor(accentColor)

        // Configurar perfil de usuario
        userProfileLayout?.setOnClickListener {
            showUserMenu(it, userNameTextView)
        }

        // Configurar notificaciones con color del rol
        notificationIcon?.setOnClickListener {
            try {
                fragment.findNavController().navigate(R.id.action_global_to_notificationsFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Aplicar tint del rol al icono de notificaciones
        notificationIcon?.setColorFilter(accentColor)
    }

    /**
     * Configuración alternativa para compatibilidad con código existente
     */
    fun setupTopBar(
        menuIcon: ImageView,
        userProfileLayout: LinearLayout,
        userNameTextView: TextView,
        notificationIcon: ImageView? = null
    ) {
        // Configurar menú hamburguesa
        menuIcon.setOnClickListener {
            (fragment.activity as? MainActivity)?.openDrawer()
        }

        // Configurar nombre de usuario
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        userNameTextView.text = "$userName $userApellido"

        // Configurar perfil de usuario
        userProfileLayout.setOnClickListener {
            showUserMenu(it, userNameTextView)
        }

        // Configurar notificaciones si existe el icono
        notificationIcon?.setOnClickListener {
            try {
                fragment.findNavController().navigate(R.id.action_global_to_notificationsFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun updateBadgeVisibility(badgeView: View?) {
        // Aquí podrías leer de una preferencia compartida o ViewModel compartido
        // Por ahora, lo manejaremos desde el NotificationsViewModel o MainActivity
    }

    private fun showUserMenu(view: View, userNameTextView: TextView?) {
        val popupMenu = PopupMenu(fragment.requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.user_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    try {
                        fragment.findNavController().navigate(R.id.action_global_to_profileFragment)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                R.id.action_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar sesión
     */
    private fun showLogoutDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cierra sesión tanto en el servidor como localmente
     */
    private fun logout() {
        fragment.lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()

                if (token != null) {
                    Log.d("TopBarManager", "🔐 Intentando cerrar sesión en servidor...")

                    // ✅ CORRECCIÓN: Crear el request con info del dispositivo
                    val request = LogoutRequest(
                        deviceModel = DeviceInfoUtil.getDeviceModel(),
                        androidVersion = DeviceInfoUtil.getAndroidVersion(),
                        sdkVersion = DeviceInfoUtil.getSdkVersion()
                    )

                    val response = apiService.logout(request)

                    if (response.isSuccessful) {
                        Log.d("TopBarManager", "✅ Sesión cerrada correctamente en servidor")
                        val body = response.body()
                        if (body?.success == true) {
                            Log.d("TopBarManager", "📝 Mensaje del servidor: ${body.message}")
                        }
                    } else {
                        Log.w("TopBarManager", "⚠️ Error al cerrar sesión en servidor: ${response.code()}")
                    }
                } else {
                    Log.w("TopBarManager", "⚠️ No hay token para invalidar")
                }

            } catch (e: Exception) {
                Log.e("TopBarManager", "❌ Error al cerrar sesión en servidor: ${e.message}")
                e.printStackTrace()
            } finally {
                // Limpiar sesión local SIEMPRE (aunque falle el servidor)
                sessionManager.clearSession()
                Log.d("TopBarManager", "🧹 Sesión local limpiada")

                // Navegar al login
                navigateToLogin()
            }
        }
    }

    /**
     * Navega a la pantalla de login
     */
    private fun navigateToLogin() {
        try {
            fragment.findNavController().navigate(R.id.action_global_to_loginFragment)
            Log.d("TopBarManager", "🧭 Navegación al login exitosa")
        } catch (e: Exception) {
            Log.e("TopBarManager", "❌ Error al navegar al login: ${e.message}")
            e.printStackTrace()

            // Si falla la navegación, cerrar la actividad
            fragment.activity?.finish()
        }
    }
}