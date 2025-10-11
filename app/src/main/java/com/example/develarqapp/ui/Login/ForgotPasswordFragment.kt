package com.example.develarqapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón enviar enlace
        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(context, "Ingresa tu correo electrónico", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Implementar lógica de recuperación de contraseña
                Toast.makeText(context, "Enlace enviado a $email", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}