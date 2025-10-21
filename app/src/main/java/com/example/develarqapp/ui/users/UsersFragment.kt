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
import com.example.develarqapp.MainActivity
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentUsersBinding
import com.example.develarqapp.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsersViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupObservers()

        // Cargar usuarios
        viewModel.loadUsers()
    }

    private fun setupUI() {
        // Configurar menú hamburguesa
        binding.ivMenuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        // Mostrar nombre del usuario logueado
        val userName = sessionManager.getUserName()
        val userApellido = sessionManager.getUserApellido()
        binding.tvUserName.text = "$userName $userApellido"

        // Botón crear usuario
        binding.btnCreateUser.setOnClickListener {
            findNavController().navigate(R.id.action_usersFragment_to_registerEmployeeFragment)
        }

        // Botón ver eliminados (por implementar)
        binding.btnViewDeleted.setOnClickListener {
            Toast.makeText(requireContext(), "Función por implementar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(
            onEditClick = { user ->
                // TODO: Navegar a pantalla de editar o mostrar dialog
                Toast.makeText(requireContext(), "Editar: ${user.fullName}", Toast.LENGTH_SHORT).show()
            },
            onToggleStatusClick = { user ->
                showToggleStatusDialog(user.id, user.fullName, user.estado)
            },
            onDeleteClick = { user ->
                showDeleteDialog(user.id, user.fullName)
            }
        )

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usersAdapter
        }
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                binding.llEmptyState.isVisible = true
                binding.rvUsers.isVisible = false
            } else {
                binding.llEmptyState.isVisible = false
                binding.rvUsers.isVisible = true
                usersAdapter.submitList(users)
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

    private fun showDeleteDialog(userId: Long, userName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Está seguro de eliminar a $userName?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteUser(userId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showToggleStatusDialog(userId: Long, userName: String, currentStatus: String) {
        val newStatus = if (currentStatus == "activo") "desactivar" else "activar"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar Estado")
            .setMessage("¿Desea $newStatus a $userName?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.toggleUserStatus(userId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}