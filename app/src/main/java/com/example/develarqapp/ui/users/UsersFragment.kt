package com.example.develarqapp.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsersViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        (activity as? MainActivity)?.updateMenuVisibility(sessionManager.getUserRol())
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        sessionManager = SessionManager(requireContext())
        val token = sessionManager.getToken()

        if (token != null) {
            viewModel.loadUsers(token)
            viewModel.loadDeletedUsers(token)

        } else {
            //findNavController().navigate(R.id.action_global_to_loginFragment)
        }
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupUI() {
        // Botón crear usuario
        binding.btnCreateUser.setOnClickListener {
            findNavController().navigate(R.id.action_usersFragment_to_registerEmployeeFragment)
        }

        // Botón ver eliminados
        binding.btnViewDeleted.setOnClickListener {
            findNavController().navigate(R.id.action_usersFragment_to_deletedUsersFragment)
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(
            onEditClick = { user ->
                // Abrir dialog de editar
                val dialog = EditUserDialogFragment.newInstance(user)
                dialog.show(childFragmentManager, "EditUserDialog")
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
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                resources.getColor(R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_green_light, null),
                resources.getColor(android.R.color.holo_orange_light, null)
            )

            setOnRefreshListener {
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.loadUsers(token)
                    viewModel.loadDeletedUsers(token)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            binding.swipeRefresh.isRefreshing = false // ✅ Detener refresh

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
           // binding.progressBar.isVisible = isLoading
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
                // CAMBIO AQUÍ: Obtener token y pasarlo
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.deleteUser(userId, token) // <-- Pasa el token
                } else {
                    // Manejar sesión expirada, quizás
                    Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showToggleStatusDialog(userId: Long, userName: String, currentStatus: String?) {
        val newStatus = if (currentStatus == "activo") "desactivar" else "activar"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar Estado")
            .setMessage("¿Desea $newStatus a $userName?")
            .setPositiveButton("Confirmar") { _, _ ->
                // CAMBIO AQUÍ: Obtener token y pasarlo
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.toggleUserStatus(userId, token) // <-- Pasa el token
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