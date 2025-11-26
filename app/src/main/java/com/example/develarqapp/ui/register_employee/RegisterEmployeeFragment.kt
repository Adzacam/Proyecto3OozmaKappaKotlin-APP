package com.example.develarqapp.ui.register_employee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentRegisterEmployeeBinding
import com.example.develarqapp.utils.SessionManager

class RegisterEmployeeFragment : Fragment() {

    private var _binding: FragmentRegisterEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterEmployeeViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRoleDropdown()
        setupObservers()
    }

    private fun setupUI() {
        // Botón volver atrás
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón registrar
        binding.btnRegister.setOnClickListener {
            registerEmployee()
        }
    }

    private fun setupRoleDropdown() {
        val roles = listOf("admin", "ingeniero", "arquitecto", "cliente")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            roles
        )
        binding.actvRole.setAdapter(adapter)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.llButtons.isVisible = !isLoading

            // Deshabilitar campos mientras carga
            binding.etName.isEnabled = !isLoading
            binding.etLastName.isEnabled = !isLoading
            binding.etEmail.isEnabled = !isLoading
            binding.etPhone.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
            binding.actvRole.isEnabled = !isLoading
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RegisterResult.Idle -> {
                    // Estado inicial
                }
                is RegisterResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                    viewModel.resetResult()
                }
                is RegisterResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetResult()
                }
            }
        }
    }

    private fun registerEmployee() {
        val name = binding.etName.text.toString()
        val apellido = binding.etLastName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val rol = binding.actvRole.text.toString()
        val token = sessionManager.getToken()
        if (token == null) {
            Toast.makeText(requireContext(), "Error de sesión, vuelve a iniciar", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_to_loginFragment) // (O a donde corresponda)
            return
        }
        viewModel.registerEmployee(
            name = name,
            apellido = apellido,
            email = email,
            phone = phone.ifEmpty { null },
            password = password,
            rol = rol,
            token= token
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}