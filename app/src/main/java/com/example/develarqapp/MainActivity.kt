package com.example.develarqapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar SessionManager
        sessionManager = SessionManager(this)

        // Inicializar vistas
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Configurar Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configurar AppBar con el drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.usersFragment,
                R.id.projectsFragment,
                R.id.kanbanFragment,
                R.id.loginFragment
            ),
            drawerLayout
        )

        // Configurar NavigationView con NavController
        navigationView.setupWithNavController(navController)

        // Actualizar header inicial
        updateNavigationHeader()

        // Manejar clics en items del menú
        setupNavigationItemListener()

        // Listener para ocultar/mostrar el drawer según el destino
        setupDestinationChangedListener()
    }

    private fun setupNavigationItemListener() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    navigateToDestination(R.id.dashboardFragment)
                    true
                }
                R.id.nav_projects -> {
                    navigateToDestination(R.id.projectsFragment)
                    true
                }
                R.id.nav_kanban -> {
                    navigateToDestination(R.id.kanbanFragment)
                    true
                }
                R.id.nav_employees -> {
                    navigateToDestination(R.id.usersFragment)
                    true
                }

                R.id.nav_profile -> {
                    // TODO: Implementar navegación a perfil
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Implementar navegación a configuración
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.forgotPasswordFragment,
                R.id.verifyEmailFragment -> {
                    // Deshabilitar el drawer en pantallas de autenticación
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    // Habilitar el drawer en otros fragmentos
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                    // Actualizar header del drawer cuando navegamos
                    updateNavigationHeader()

                    // Actualizar visibilidad del menú según rol
                    val userRole = sessionManager.getUserRol()
                    if (userRole.isNotEmpty()) {
                        updateMenuVisibility(userRole)
                    }
                }
            }
        }
    }

    private fun navigateToDestination(destinationId: Int) {
        try {
            // Verificar si no estamos ya en ese destino
            if (navController.currentDestination?.id != destinationId) {
                navController.navigate(destinationId)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        } catch (e: Exception) {
            e.printStackTrace()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    // Hacer pública para que DashboardFragment pueda llamarla
    fun updateMenuVisibility(role: String) {
        val menu = navigationView.menu

        when (role.lowercase()) {
            "admin" -> {
                // Admin puede ver todo
                menu.findItem(R.id.nav_dashboard)?.isVisible = true
                menu.findItem(R.id.nav_projects)?.isVisible = true
                menu.findItem(R.id.nav_kanban)?.isVisible = true
                menu.findItem(R.id.nav_employees)?.isVisible = true

            }
            "ingeniero", "arquitecto" -> {
                // Ingenieros y arquitectos ven proyectos y kanban, pero no gestión de empleados
                menu.findItem(R.id.nav_dashboard)?.isVisible = true
                menu.findItem(R.id.nav_projects)?.isVisible = true
                menu.findItem(R.id.nav_kanban)?.isVisible = true
                menu.findItem(R.id.nav_employees)?.isVisible = false

            }
            "cliente" -> {
                // Clientes solo ven dashboard y proyectos
                menu.findItem(R.id.nav_dashboard)?.isVisible = true
                menu.findItem(R.id.nav_projects)?.isVisible = true
                menu.findItem(R.id.nav_kanban)?.isVisible = false
                menu.findItem(R.id.nav_employees)?.isVisible = false

            }
            else -> {
                // Otros roles solo ven lo básico
                menu.findItem(R.id.nav_dashboard)?.isVisible = true
                menu.findItem(R.id.nav_projects)?.isVisible = false
                menu.findItem(R.id.nav_kanban)?.isVisible = false
                menu.findItem(R.id.nav_employees)?.isVisible = false

            }
        }
    }

    private fun updateNavigationHeader() {
        try {
            val headerView = navigationView.getHeaderView(0)
            val tvNavUserName: TextView = headerView.findViewById(R.id.tvNavUserName)
            val tvNavUserRole: TextView = headerView.findViewById(R.id.tvNavUserRole)

            if (sessionManager.isLoggedIn()) {
                val userName = sessionManager.getUserName()
                val userApellido = sessionManager.getUserApellido()
                val userRole = sessionManager.getUserRol()

                // Actualizar nombre
                tvNavUserName.text = if (userName.isNotEmpty() && userApellido.isNotEmpty()) {
                    "$userName $userApellido"
                } else {
                    "Usuario"
                }

                // Actualizar rol con formato bonito
                tvNavUserRole.text = when (userRole.lowercase()) {
                    "admin" -> "Administrador"
                    "ingeniero" -> "Ingeniero"
                    "arquitecto" -> "Arquitecto"
                    "cliente" -> "Cliente"
                    else -> if (userRole.isNotEmpty()) {
                        userRole.replaceFirstChar { it.uppercase() }
                    } else {
                        "Rol"
                    }
                }
            } else {
                tvNavUserName.text = "Usuario"
                tvNavUserRole.text = "Invitado"
            }
        } catch (e: Exception) {
            // Si hay error al obtener el header, simplemente no actualizamos
            e.printStackTrace()
        }
    }

    private fun logout() {
        try {
            // Limpiar sesión
            sessionManager.clearSession()

            // Cerrar drawer
            drawerLayout.closeDrawer(GravityCompat.START)

            // Navegar al login y limpiar el back stack
            navController.navigate(R.id.action_dashboardFragment_to_loginFragment)
        } catch (e: Exception) {
            // Si falla la navegación con action, intentar navegación directa
            e.printStackTrace()
            navController.navigate(R.id.loginFragment)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            navController.currentDestination?.id == R.id.dashboardFragment -> {
                // Si estamos en dashboard, salir de la app
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    // Funciones públicas para ser llamadas desde fragments

    fun openDrawer() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    fun refreshNavigationHeader() {
        updateNavigationHeader()
    }

    fun refreshMenuVisibility() {
        val userRole = sessionManager.getUserRol()
        if (userRole.isNotEmpty()) {
            updateMenuVisibility(userRole)
        }
    }
}