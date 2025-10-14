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
import com.example.develarqapp.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendResetLink(email)
        }

        binding.btnCancel.setOnClickListener {
            // Vuelve a la pantalla anterior (login)
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Podrías añadir un ProgressBar a tu XML si lo deseas
            binding.btnSendResetLink.isEnabled = !isLoading
            binding.btnCancel.isEnabled = !isLoading
        }

        viewModel.resetState.observe(viewLifecycleOwner) { state ->
            when(state) {
                is ForgotPasswordState.Success -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    // Después de un éxito, volvemos al login
                    findNavController().popBackStack()
                }
                is ForgotPasswordState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                is ForgotPasswordState.Idle -> { /* No hacer nada */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}