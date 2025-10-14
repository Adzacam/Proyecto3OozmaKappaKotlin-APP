package com.example.develarqapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.example.develarqapp.utils.SessionManager // 1. Importar SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // 2. Declarar la variable para SessionManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 3. Inicializar SessionManager
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
                R.id.loginFragment
            ),
            drawerLayout
        )

        // Configurar NavigationView con NavController
        navigationView.setupWithNavController(navController)

        // Manejar clics en items del menú
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Primero cerramos el drawer para una transición más limpia
            drawerLayout.closeDrawer(GravityCompat.START)

            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    navController.navigate(R.id.dashboardFragment)
                    true
                }
                R.id.nav_projects -> {
                    // TODO: Navegar a proyectos cuando esté implementado
                    true
                }
                R.id.nav_employees -> {
                    // TODO: Navegar a empleados cuando esté implementado
                    true
                }
                R.id.nav_register_employee -> {
                    navController.navigate(R.id.registerEmployeeFragment)
                    true
                }
                R.id.nav_profile -> {
                    // TODO: Navegar a perfil cuando esté implementado
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Navegar a configuración cuando esté implementado
                    true
                }
                R.id.nav_logout -> {
                    // 4. LÓGICA COMPLETA PARA CERRAR SESIÓN
                    // Primero, borrar todos los datos guardados de la sesión.
                    sessionManager.clearSession()

                    // Luego, navegar a la pantalla de login.
                    // Usamos la acción específica del nav_graph que limpia el historial
                    // para que el usuario no pueda volver al dashboard.
                    navController.navigate(R.id.action_dashboardFragment_to_loginFragment)
                    true
                }
                else -> false
            }
        }

        // Listener para ocultar/mostrar el drawer según el destino
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.forgotPasswordFragment -> {
                    // Deshabilitar el drawer en login y forgot password
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    // Habilitar el drawer en otros fragmentos
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Función auxiliar para abrir el drawer desde fragmentos
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    // Función auxiliar para cerrar el drawer desde fragmentos
    fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }
}