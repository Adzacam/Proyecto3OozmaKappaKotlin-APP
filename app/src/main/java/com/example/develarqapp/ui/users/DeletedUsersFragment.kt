package com.example.develarqapp.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.databinding.FragmentDeletedUsersBinding
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeletedUsersFragment : Fragment() {

    private var _binding: FragmentDeletedUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsersViewModel by viewModels()
    private lateinit var deletedUsersAdapter: DeletedUsersAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeletedUsersBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext()) // <-- 2. INICIALIZAR
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        setupRecyclerView()
        setupObservers()

        // Cargar usuarios eliminados
        val token = sessionManager.getToken()
        if (token != null) {
            viewModel.loadDeletedUsers(token)
        } else {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp() // Salir si no hay sesión
        }
    }

    private fun setupRecyclerView() {
        deletedUsersAdapter = DeletedUsersAdapter { user ->
            showRestoreDialog(user.id, user.fullName)
        }

        binding.rvDeletedUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deletedUsersAdapter
        }
    }

    private fun setupObservers() {
        // ... (Tu código de observers no cambia) ...
        viewModel.deletedUsers.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                binding.llEmptyState.isVisible = true
                binding.rvDeletedUsers.isVisible = false
            } else {
                binding.llEmptyState.isVisible = false
                binding.rvDeletedUsers.isVisible = true
                deletedUsersAdapter.submitList(users)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    private fun showRestoreDialog(userId: Long, userName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restaurar Usuario")
            .setMessage("¿Está seguro de restaurar a $userName?")
            .setPositiveButton("Restaurar") { _, _ ->
                val token = sessionManager.getToken() // <-- 5. OBTENER TOKEN
                if (token != null) {
                    viewModel.restoreUser(userId, token) // <-- 6. PASAR TOKEN
                } else {
                    Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}