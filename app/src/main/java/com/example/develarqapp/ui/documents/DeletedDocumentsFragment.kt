package com.example.develarqapp.ui.documents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.databinding.FragmentDeletedDocumentsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeletedDocumentsFragment : Fragment() {

    private var _binding: FragmentDeletedDocumentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private val viewModel: DocumentsViewModel by viewModels()
    private lateinit var adapter: DeletedDocumentsAdapter

    private var filteredDocuments: List<Document> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeletedDocumentsBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Cargar documentos eliminados
        viewModel.loadDeletedDocuments()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupRecyclerView() {
        adapter = DeletedDocumentsAdapter(
            onRestoreClick = { document -> confirmRestore(document) },
            onPermanentDeleteClick = { document -> confirmPermanentDelete(document) }
        )

        binding.rvDeletedDocuments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DeletedDocumentsFragment.adapter
        }
    }

    private fun setupObservers() {
        // Documentos eliminados
        viewModel.deletedDocuments.observe(viewLifecycleOwner) { documents ->
            filteredDocuments = documents
            applySearchFilter()
        }

        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Mensajes
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
                viewModel.loadDeletedDocuments() // Recargar lista
            }
        }
    }

    private fun setupListeners() {
        // Regresar
        binding.btnRegresar.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Purgar documentos antiguos
        binding.btnPurgar.setOnClickListener {
            confirmPurgeOldDocuments()
        }

        // Búsqueda
        binding.etSearch.doAfterTextChanged {
            applySearchFilter()
        }
    }

    private fun applySearchFilter() {
        val query = binding.etSearch.text.toString().trim()

        val filtered = if (query.isBlank()) {
            filteredDocuments
        } else {
            filteredDocuments.filter { document ->
                document.nombre.contains(query, ignoreCase = true) ||
                        document.descripcion?.contains(query, ignoreCase = true) == true ||
                        document.proyectoNombre?.contains(query, ignoreCase = true) == true
            }
        }

        if (filtered.isEmpty()) {
            binding.tvPlaceholder.visibility = View.VISIBLE
            binding.rvDeletedDocuments.visibility = View.GONE
            binding.tvPlaceholder.text = if (query.isBlank()) {
                "No hay documentos en la papelera"
            } else {
                "No se encontraron documentos con '$query'"
            }
        } else {
            binding.tvPlaceholder.visibility = View.GONE
            binding.rvDeletedDocuments.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun confirmRestore(document: Document) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restaurar Documento")
            .setMessage("¿Deseas restaurar '${document.nombre}'?")
            .setPositiveButton("Restaurar") { _, _ ->
                viewModel.restoreDocument(document.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmPermanentDelete(document: Document) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Eliminar Permanentemente")
            .setMessage("Esta acción NO se puede deshacer. ¿Estás seguro de eliminar permanentemente '${document.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Aquí necesitarías un endpoint para eliminación permanente
                Toast.makeText(requireContext(), "Función de eliminación permanente próximamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmPurgeOldDocuments() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Purgar Documentos Antiguos")
            .setMessage("Esto eliminará PERMANENTEMENTE todos los documentos que llevan más de 30 días en la papelera. ¿Continuar?")
            .setPositiveButton("Purgar") { _, _ ->
                // Aquí necesitarías un endpoint para purgar
                Toast.makeText(requireContext(), "Función de purga próximamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}