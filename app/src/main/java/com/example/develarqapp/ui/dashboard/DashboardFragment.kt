package com.example.develarqapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.develarqapp.databinding.FragmentDashboardBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar los datos del usuario en el ViewModel
        viewModel.loadUserData(sessionManager)

        setupTopBar()
        setupUI()
        observeViewModel()
    }

    private fun setupTopBar() {
        // Usa el acceso directo de ViewBinding, es más limpio y seguro
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupUI() {
        // Configuración adicional del dashboard si es necesaria
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            val fullName = "$name ${sessionManager.getUserApellido()}"
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