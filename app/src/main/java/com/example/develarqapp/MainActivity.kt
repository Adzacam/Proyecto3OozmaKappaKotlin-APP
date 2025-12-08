package com.example.develarqapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.example.develarqapp.databinding.ActivityMainBinding
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import android.widget.TextView
import com.example.develarqapp.utils.RoleColorsHelper
import android.media.MediaPlayer
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.example.develarqapp.data.api.ApiConfig
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sessionManager: SessionManager
    private val apiService by lazy { ApiConfig.getApiService() }
    private var mediaPlayer: MediaPlayer? = null

    private val notificationHandler = android.os.Handler(Looper.getMainLooper())
    private val checkNotificationsRunnable = object : Runnable {
        override fun run() {
            checkUnreadNotifications()
            // Verificar cada 30 segundos
            notificationHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        mediaPlayer = MediaPlayer.create(this, R.raw.notification_sound)
        if (sessionManager.getToken() != null) {
            startNotificationCheck()
        }
        setupNavigation()
        updateMenuVisibility(sessionManager.getUserRol())

    }
    private fun startNotificationCheck() {
        notificationHandler.post(checkNotificationsRunnable)
    }

    private fun stopNotificationCheck() {
        notificationHandler.removeCallbacks(checkNotificationsRunnable)
    }
    private fun checkUnreadNotifications() {
        val token = sessionManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                // Llamamos solo para ver el conteo de no leídas
                val response = apiService.getNotifications(
                    token = "Bearer $token",
                    limit = 1 // Solo necesitamos saber el conteo del header
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val unreadCount = response.body()?.noLeidas ?: 0

                    updateNotificationBadge(unreadCount > 0)

                    val lastCount = sessionManager.getLastUnreadCount() // Necesitas agregar esto a SessionManager
                    if (unreadCount > lastCount) {
                        playSound()
                    }
                    sessionManager.saveLastUnreadCount(unreadCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun updateNotificationBadge(show: Boolean) {
        // Buscar el badge en el fragmento actual o en la Toolbar de la Activity si es global
        val badge = findViewById<View>(R.id.viewNotificationBadge)
        badge?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun playSound() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.seekTo(0)
            } else {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
        navigationView.setNavigationItemSelectedListener(this)

        // Configurar el drawer solo para destinos principales
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.usersFragment,
                R.id.projectsFragment,
                R.id.kanbanFragment,
                R.id.calendarFragment,
                R.id.documentsFragment,
                R.id.downloadHistoryFragment,
                R.id.auditoriaFragment,
                R.id.bimPlanosFragment,
            ),
            drawerLayout
        )


        setupNavigationListener()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.forgotPasswordFragment,
                R.id.verifyEmailFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }
    }

    /**
     * Actualiza la visibilidad de los items del menú según el rol del usuario
     */
    fun updateMenuVisibility(role: String) {
        val menu = navigationView.menu
        val accentColor = RoleColorsHelper.getAccentColor(this, role)

        when (role.lowercase()) {
            "admin" -> {
                menu.findItem(R.id.nav_employees)?.isVisible = true
                menu.findItem(R.id.nav_calendar)?.isVisible = true
                menu.findItem(R.id.nav_documents)?.isVisible = true
                menu.findItem(R.id.nav_download_history)?.isVisible = true
                menu.findItem(R.id.nav_auditoria)?.isVisible = true
                menu.findItem(R.id.nav_bim_plans)?.isVisible = true
            }
            "ingeniero", "arquitecto" -> {
                menu.findItem(R.id.nav_employees)?.isVisible = false
                menu.findItem(R.id.nav_calendar)?.isVisible = true
                menu.findItem(R.id.nav_documents)?.isVisible = true
                menu.findItem(R.id.nav_download_history)?.isVisible = false
                menu.findItem(R.id.nav_auditoria)?.isVisible = false
                menu.findItem(R.id.nav_bim_plans)?.isVisible = true
            }
            "cliente" -> {
                menu.findItem(R.id.nav_employees)?.isVisible = false
                menu.findItem(R.id.nav_calendar)?.isVisible = false
                menu.findItem(R.id.nav_documents)?.isVisible = false
                menu.findItem(R.id.nav_download_history)?.isVisible = false
                menu.findItem(R.id.nav_auditoria)?.isVisible = false
                menu.findItem(R.id.nav_bim_plans)?.isVisible = false
            }
        }

        // Actualizar la Cabecera con colores del rol
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvNavUserName)
        val tvUserRole = headerView.findViewById<TextView>(R.id.tvNavUserRole)

        // Datos del usuario
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        val capitalizedRole = RoleColorsHelper.getRoleDisplayName(role)

        tvUserName.text = "$userName $userApellido"
        tvUserRole.text = capitalizedRole
        tvUserRole.setTextColor(accentColor)


        applyRoleColorToMenuIcons(menu, accentColor)

        val currentDestination = navController.currentDestination?.id
        currentDestination?.let { destId ->
            when (destId) {
                R.id.dashboardFragment -> highlightActiveMenuItem(R.id.nav_dashboard)
                R.id.usersFragment -> highlightActiveMenuItem(R.id.nav_employees)
                R.id.projectsFragment -> highlightActiveMenuItem(R.id.nav_projects)
                R.id.kanbanFragment -> highlightActiveMenuItem(R.id.nav_kanban)
                R.id.calendarFragment -> highlightActiveMenuItem(R.id.nav_calendar)
                R.id.documentsFragment -> highlightActiveMenuItem(R.id.nav_documents)
                R.id.downloadHistoryFragment -> highlightActiveMenuItem(R.id.nav_download_history)
                R.id.auditoriaFragment -> highlightActiveMenuItem(R.id.nav_auditoria)
                R.id.bimPlanosFragment -> highlightActiveMenuItem(R.id.nav_bim_plans)
            }
        }
    }

    /**
     * Aplicar color del rol a los iconos del menú
     */
    private fun applyRoleColorToMenuIcons(menu: Menu, color: Int) {
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)

            // Aplicar color a items principales
            menuItem.icon?.setTint(color)

            // Aplicar color a submenús
            if (menuItem.hasSubMenu()) {
                val subMenu = menuItem.subMenu
                subMenu?.let {
                    for (j in 0 until it.size()) {
                        it.getItem(j).icon?.setTint(color)
                    }
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // ✅ Resaltar el item seleccionado inmediatamente
        highlightActiveMenuItem(item.itemId)

        when (item.itemId) {
            R.id.nav_dashboard -> {
                navController.navigate(R.id.dashboardFragment)
            }
            R.id.nav_employees -> {
                if (hasAccess("admin")) {
                    navController.navigate(R.id.usersFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_projects -> {
                navController.navigate(R.id.projectsFragment)
            }
            R.id.nav_kanban -> {
                navController.navigate(R.id.kanbanFragment)
            }
            R.id.nav_calendar -> {
                if (hasAccess("admin", "ingeniero", "arquitecto")) {
                    navController.navigate(R.id.calendarFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_documents -> {
                if (hasAccess("admin", "ingeniero", "arquitecto")) {
                    navController.navigate(R.id.documentsFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_download_history -> {
                if (hasAccess("admin")) {
                    navController.navigate(R.id.downloadHistoryFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_bim_plans -> {
                if (hasAccess("admin", "ingeniero", "arquitecto")) {
                    navController.navigate(R.id.bimPlanosFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_auditoria -> {
                if (hasAccess("admin")) {
                    navController.navigate(R.id.auditoriaFragment)
                } else {
                    showAccessDeniedDialog()
                }
            }
            R.id.nav_profile -> {
                navController.navigate(R.id.action_global_to_profileFragment)
            }
            R.id.nav_notifications -> {
                navController.navigate(R.id.action_global_to_notificationsFragment)
            }
            R.id.nav_settings -> {

            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Resalta el item del menú activo con el color del rol
     */
    private fun highlightActiveMenuItem(itemId: Int) {
        val menu = navigationView.menu
        val userRole = sessionManager.getUserRol()
        val accentColor = RoleColorsHelper.getAccentColor(this, userRole)
        val backgroundColor = RoleColorsHelper.getAccentBackgroundColor(this, userRole)

        // Resetear todos los items
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)

            // Resetear submenús también
            if (menuItem.hasSubMenu()) {
                val subMenu = menuItem.subMenu
                subMenu?.let {
                    for (j in 0 until it.size()) {
                        resetMenuItem(it.getItem(j))
                    }
                }
            } else {
                resetMenuItem(menuItem)
            }
        }

        // Resaltar el item activo
        val activeItem = menu.findItem(itemId)
        activeItem?.let {
            it.isChecked = true

            // Aplicar color del rol al icono y texto
            it.icon?.setTint(accentColor)

            // Aplicar color de fondo al item seleccionado
            val itemView = navigationView.findViewById<View>(itemId)
            itemView?.setBackgroundColor(backgroundColor)
        }
    }

    /**
     * Resetear el estilo de un item del menú
     */
    private fun resetMenuItem(menuItem: MenuItem) {
        menuItem.isChecked = false
        menuItem.icon?.setTintList(null) // Resetear a color original
    }

    /**
     * Configurar el listener de cambios de destino para resaltar automáticamente
     */
    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment -> highlightActiveMenuItem(R.id.nav_dashboard)
                R.id.usersFragment -> highlightActiveMenuItem(R.id.nav_employees)
                R.id.projectsFragment -> highlightActiveMenuItem(R.id.nav_projects)
                R.id.kanbanFragment -> highlightActiveMenuItem(R.id.nav_kanban)
                R.id.calendarFragment -> highlightActiveMenuItem(R.id.nav_calendar)
                R.id.documentsFragment -> highlightActiveMenuItem(R.id.nav_documents)
                R.id.downloadHistoryFragment -> highlightActiveMenuItem(R.id.nav_download_history)
                R.id.auditoriaFragment -> highlightActiveMenuItem(R.id.nav_auditoria)
                R.id.bimPlanosFragment -> highlightActiveMenuItem(R.id.nav_bim_plans)
                R.id.profileFragment -> highlightActiveMenuItem(R.id.nav_profile)
                R.id.notificationsFragment -> highlightActiveMenuItem(R.id.nav_notifications)
            }
        }
    }

    /**
     * Verifica si el usuario tiene acceso según su rol
     */
    private fun hasAccess(vararg allowedRoles: String): Boolean {
        val userRole = sessionManager.getUserRol().lowercase()
        return allowedRoles.any { it.lowercase() == userRole }
    }

    /**
     * Muestra un diálogo de acceso denegado
     */
    private fun showAccessDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Acceso Denegado")
            .setMessage("No tienes permisos para acceder a esta sección.")
            .setPositiveButton("Entendido", null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación para cerrar sesión
     */
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cierra la sesión del usuario
     */
    private fun logout() {
        sessionManager.clearSession()
        navController.navigate(R.id.action_global_to_loginFragment)
    }

    /**
     * Abre el drawer desde los fragments
     */
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopNotificationCheck()
        mediaPlayer?.release()
    }
}