package com.example.develarqapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentLoginBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // 1. Obtener una instancia del ViewModel
    private val viewModel: LoginViewModel by viewModels()

    // 2. Instanciar el SessionManager
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, //
        container: ViewGroup?, //
        savedInstanceState: Bundle? //
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false) //
        sessionManager = SessionManager(requireContext()) //

        if (sessionManager.isLoggedIn()) {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment) //
        }

        return binding.root //
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { //
        super.onViewCreated(view, savedInstanceState) //

        // Solo configurar listeners y observers si el usuario no ha iniciado sesión
        if (!sessionManager.isLoggedIn()) {
            setupClickListeners() //
            observeViewModel() //
            loadRememberedEmail()
        }
    }

    private fun loadRememberedEmail() {
        val rememberedEmail = sessionManager.getRememberedEmail()
        if (rememberedEmail != null) {
            binding.etEmail.setText(rememberedEmail)
            binding.cbRememberMe.isChecked = true
        }
    }

    private fun setupClickListeners() {
        // 3. Configurar el click del botón de login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    // 4. Observar los cambios de estado desde el ViewModel
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Muestra u oculta el ProgressBar y habilita/deshabilita el botón
            binding.progressBar.isVisible = isLoading
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state -> //
            when (state) { //
                is LoginState.Success -> { //
                    Toast.makeText(context, "¡Bienvenido ${state.userData.name}!", Toast.LENGTH_SHORT).show() //

                    // --- GUARDAR PREFERENCIA DE "RECORDARME" ---
                    val rememberMe = binding.cbRememberMe.isChecked
                    val email = binding.etEmail.text.toString().trim()
                    sessionManager.saveRememberMePreference(email, rememberMe)

                    // Guardar los datos de sesión y el token
                    sessionManager.saveAuthToken(state.token) //
                    sessionManager.saveUserData( //
                        id = state.userData.id, //
                        name = state.userData.name, //
                        apellido = state.userData.apellido, //
                        email = state.userData.email, //
                        rol = state.userData.rol //
                    )

                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment) //
                    viewModel.resetLoginState() //
                }
                is LoginState.Error -> { //
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show() //
                }
                is LoginState.Idle -> { //
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}