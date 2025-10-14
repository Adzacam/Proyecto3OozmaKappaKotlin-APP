package com.example.develarqapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentDashboardBinding
import com.example.develarqapp.utils.SessionManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null //
    private val binding get() = _binding!! //

    // Obtener instancias del ViewModel y SessionManager
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, //
        container: ViewGroup?, //
        savedInstanceState: Bundle? //
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false) //
        sessionManager = SessionManager(requireContext())
        return binding.root //
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { //
        super.onViewCreated(view, savedInstanceState) //

        // Cargar los datos del usuario en el ViewModel
        viewModel.loadUserData(sessionManager)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Configurar el botón del menú para abrir el drawer
        binding.ivMenuIcon.setOnClickListener { //
            (activity as? MainActivity)?.openDrawer() //
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            val fullName = "$name ${sessionManager.getUserApellido()}"
            binding.tvUserName.text = fullName //
            binding.tvWelcome.text = "Bienvenido, $fullName" //
        }

        viewModel.userRole.observe(viewLifecycleOwner) { role ->
            // Capitalizar el rol para mostrarlo (ej: "ingeniero" -> "Ingeniero")
            val capitalizedRole = role.replaceFirstChar { it.uppercase() }
            binding.tvRole.text = capitalizedRole //

            // Mostrar contenido específico según el rol
            updateUIForRole(role)
        }
    }

    private fun updateUIForRole(role: String) {
        // Ocultar todas las secciones específicas primero
        binding.cvEngineerSection.isVisible = false //
        // Aquí podrías ocultar otras tarjetas (ej: cvAdminSection, cvClientSection)

        // Mostrar la sección correspondiente al rol del usuario
        when (role) {
            "admin" -> {
                // Aquí podrías hacer visible una tarjeta para administradores
                // binding.cvAdminSection.isVisible = true
            }
            "ingeniero" -> {
                binding.cvEngineerSection.isVisible = true //
            }
            "arquitecto" -> {
                // Aquí podrías configurar y mostrar una tarjeta para arquitectos
            }
            "cliente" -> {
                // Y aquí una para clientes
            }
        }
    }

    override fun onDestroyView() { //
        super.onDestroyView() //
        _binding = null //
    }
}