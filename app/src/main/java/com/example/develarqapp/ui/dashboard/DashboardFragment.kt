package com.example.develarqapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentDashboardBinding
import com.example.develarqapp.utils.SessionManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar los datos del usuario en el ViewModel
        viewModel.loadUserData(sessionManager)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Configurar el botón del menú hamburguesa para abrir el drawer
        binding.ivMenuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        // Configurar el menú desplegable del usuario
        binding.llUserProfile.setOnClickListener {
            showUserMenu(it)
        }
    }

    private fun showUserMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.user_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    // TODO: Navegar a perfil
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
        // Limpiar sesión
        sessionManager.clearSession()

        // Navegar al login
        findNavController().navigate(R.id.action_dashboardFragment_to_loginFragment)
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            val fullName = "$name ${sessionManager.getUserApellido()}"
            binding.tvUserName.text = fullName
            binding.tvWelcome.text = "Bienvenido, $fullName"
        }

        viewModel.userRole.observe(viewLifecycleOwner) { role ->
            val capitalizedRole = role.replaceFirstChar { it.uppercase() }
            binding.tvRole.text = capitalizedRole

            // Mostrar contenido específico según el rol
            updateUIForRole(role)

            // Actualizar el navigation drawer según el rol
            updateNavigationMenu(role)
        }
    }

    private fun updateUIForRole(role: String) {
        binding.cvEngineerSection.isVisible = false

        when (role) {
            "admin" -> {
                // Mostrar sección de admin si existe
            }
            "ingeniero" -> {
                binding.cvEngineerSection.isVisible = true
            }
            "arquitecto" -> {
                // Mostrar sección de arquitecto
            }
            "cliente" -> {
                // Mostrar sección de cliente
            }
        }
    }

    private fun updateNavigationMenu(role: String) {
        (activity as? MainActivity)?.updateMenuVisibility(role)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}