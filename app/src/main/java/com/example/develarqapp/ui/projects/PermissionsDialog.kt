package com.example.develarqapp.ui.projects

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogPermissionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionsDialog : DialogFragment() {

    private var _binding: DialogPermissionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })
    private lateinit var permissionsAdapter: PermissionsAdapter

    private var projectId: Long = 0

    companion object {
        fun newInstance(projectId: Long): PermissionsDialog {
            val fragment = PermissionsDialog()
            val args = Bundle().apply {
                putLong("projectId", projectId)
            }
            fragment.arguments = args
            return fragment
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener argumentos
        arguments?.let {
            projectId = it.getLong("projectId")
        }

        setupUI()
        setupRecyclerView()
        observeViewModel()

        // Cargar permisos
        viewModel.loadProjectPermissions(projectId)
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { savePermissions() }
    }

    private fun setupRecyclerView() {
        permissionsAdapter = PermissionsAdapter()

        binding.rvPermissions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = permissionsAdapter
        }
    }

    private fun savePermissions() {
        val updatedPermissions = permissionsAdapter.getUpdatedPermissions()

        if (updatedPermissions.isEmpty()) {
            Toast.makeText(requireContext(), "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateProjectPermissions(projectId, updatedPermissions)
    }

    private fun observeViewModel() {
        viewModel.permissions.observe(viewLifecycleOwner) { permissions ->
            permissionsAdapter.submitList(permissions)
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}