package com.example.develarqapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.develarqapp.MainActivity
import com.example.develarqapp.databinding.FragmentProfileBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.example.develarqapp.R

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        setupUI()
        observeViewModel()

        // Cargar datos del usuario
        viewModel.loadUserProfile(sessionManager)
    }

    private fun setupTopBar() {
        // Usa el acceso directo de ViewBinding, es más limpio y seguro
        topBarManager.setupTopBar(binding.topAppBar.root)
    }


    private fun setupUI() {
        // Botón guardar información del perfil
        binding.btnGuardarPerfil.setOnClickListener {
            val newName = binding.etNombre.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateUserName(newName)
            } else {
                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón guardar contraseña
        binding.btnGuardarPassword.setOnClickListener {
            val currentPassword = binding.etPasswordActual.text.toString()
            val newPassword = binding.etPasswordNueva.text.toString()
            val confirmPassword = binding.etPasswordConfirmar.text.toString()

            when {
                currentPassword.isEmpty() -> {
                    Toast.makeText(requireContext(), "Ingrese su contraseña actual", Toast.LENGTH_SHORT).show()
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(requireContext(), "Ingrese una nueva contraseña", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 8 -> {
                    Toast.makeText(requireContext(), "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    viewModel.updatePassword(currentPassword, newPassword)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.etNombre.setText(name)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnGuardarPerfil.isEnabled = !isLoading
            binding.btnGuardarPassword.isEnabled = !isLoading
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()

                // Limpiar campos de contraseña después de actualizar
                if (it.contains("contraseña", ignoreCase = true)) {
                    binding.etPasswordActual.text?.clear()
                    binding.etPasswordNueva.text?.clear()
                    binding.etPasswordConfirmar.text?.clear()
                }

                viewModel.clearSuccess()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}