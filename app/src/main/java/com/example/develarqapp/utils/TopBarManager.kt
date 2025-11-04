package com.example.develarqapp.utils

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R

/**
 * Clase utilitaria para configurar el TopBar de forma consistente en todos los fragments
 */
class TopBarManager(
    private val fragment: Fragment,
    private val sessionManager: SessionManager
) {

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

        // Configurar nombre de usuario
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        userNameTextView?.text = "$userName $userApellido"

        // Configurar perfil de usuario
        userProfileLayout?.setOnClickListener {
            showUserMenu(it, userNameTextView)
        }

        // Configurar notificaciones
        notificationIcon?.setOnClickListener {
            try {
                fragment.findNavController().navigate(R.id.action_global_to_notificationsFragment)
            } catch (e: Exception) {
                // Si falla la navegación global, intentar navegar directamente
                e.printStackTrace()
            }
        }
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
                    logout()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun logout() {
        sessionManager.clearSession()
        try {
            fragment.findNavController().navigate(R.id.action_global_to_loginFragment)
        } catch (e: Exception) {
            e.printStackTrace()
            fragment.activity?.finish()
        }
    }
}