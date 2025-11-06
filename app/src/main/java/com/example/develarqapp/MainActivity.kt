package com.example.develarqapp

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.develarqapp.databinding.ActivityMainBinding
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import android.widget.TextView
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupNavigation()
        updateMenuVisibility(sessionManager.getUserRol())

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

        // Ocultar/mostrar drawer según el destino
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
        // --- 2. [NUEVO] Código para actualizar la Cabecera ---
        val headerView = navigationView.getHeaderView(0) // Obtiene nav_header.xml
        val tvUserName = headerView.findViewById<TextView>(R.id.tvNavUserName)
        val tvUserRole = headerView.findViewById<TextView>(R.id.tvNavUserRole)

        // Asumiendo que SessionManager tiene estos métodos
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        val capitalizedRole = role.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        tvUserName.text = "$userName $userApellido"
        tvUserRole.text = capitalizedRole
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
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
                // TODO: Implementar configuración
            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
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
}