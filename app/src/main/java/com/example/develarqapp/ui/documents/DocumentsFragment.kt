package com.example.develarqapp.ui.documents

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentFilters
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.databinding.FragmentDocumentsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DocumentsFragment : Fragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private val viewModel: DocumentsViewModel by activityViewModels()
    private lateinit var adapter: DocumentsAdapter

    private var selectedFileUri: Uri? = null

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                showUploadDialog(selectedFileUri)
            }
        }
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFilePicker()
        } else {
            Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentsBinding.inflate(inflater, container, false)
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
        setupFilters()

        // Cargar datos iniciales
        viewModel.loadDocuments()
        viewModel.loadProjects()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupRecyclerView() {
        adapter = DocumentsAdapter(
            onDownloadClick = { document -> downloadDocument(document) },
            onEditClick = { document -> showEditDialog(document) },
            onDeleteClick = { document -> confirmDelete(document) }
        )

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DocumentsFragment.adapter
        }
    }

    private fun setupObservers() {
        // Documentos filtrados
        viewModel.filteredDocuments.observe(viewLifecycleOwner) { documents ->
            // Agrega este log para depurar
            android.util.Log.d("DEBUG_DOCS", "Recibidos ${documents.size} documentos")

            if (documents.isEmpty()) {
                binding.tvPlaceholder.visibility = View.VISIBLE
                binding.rvDocuments.visibility = View.GONE
            } else {
                binding.tvPlaceholder.visibility = View.GONE
                binding.rvDocuments.visibility = View.VISIBLE
                adapter.submitList(documents)
            }
        }

        // Proyectos para filtro
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            val projectNames = listOf("Todos los Proyectos") + projects.map { it.nombre }
            val projectAdapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_dark,
                projectNames
            )
            projectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
            binding.spinnerProyecto.adapter = projectAdapter
        }

        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Mensajes de error
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // Mensajes de éxito
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // Archivo descargado
        viewModel.downloadedFile.observe(viewLifecycleOwner) { file ->
            Toast.makeText(
                requireContext(),
                "Archivo guardado: ${file.name}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupListeners() {
        // Subir documento
        binding.btnSubirDocumento.setOnClickListener {
            showUploadDialog()
        }

        // Ver papelera
        binding.btnVerPapelera.setOnClickListener {
            navigateToTrash()
        }

        // Búsqueda con cambio de texto
        binding.etSearch.doAfterTextChanged { text ->
            val query = text.toString().trim()
            val currentFilters = viewModel.filters.value ?: DocumentFilters()
            currentFilters.searchQuery = query
            viewModel.updateFilters(currentFilters)
        }

        // Filtro por tipo
        binding.spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val currentFilters = viewModel.filters.value ?: DocumentFilters()
                currentFilters.selectedType = if (position > 0) {
                    DocumentType.values()[position - 1]
                } else {
                    null
                }
                viewModel.updateFilters(currentFilters)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Filtro por proyecto
        binding.spinnerProyecto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val currentFilters = viewModel.filters.value ?: DocumentFilters()
                val projects = viewModel.projects.value ?: emptyList()

                currentFilters.selectedProjectId = if (position > 0 && projects.isNotEmpty()) {
                    projects[position - 1].id
                } else {
                    null
                }
                viewModel.updateFilters(currentFilters)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Limpiar filtros
        binding.btnLimpiarFiltros.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerTipo.setSelection(0)
            binding.spinnerProyecto.setSelection(0)
            viewModel.clearFilters()
        }
    }

    private fun setupFilters() {
        // Filtro tipo
        val types = listOf("Todos los Tipos") + DocumentType.values().map { it.displayName }
        val typeAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_dark,
            types
        )
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spinnerTipo.adapter = typeAdapter
    }

    private fun checkPermissionsAndOpenPicker() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ no requiere permisos para ACTION_GET_CONTENT
                openFilePicker()
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openFilePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    private fun showUploadDialog(fileUri: Uri? = null) {
        val dialog = UploadDocumentDialogFragment.newInstance(fileUri = fileUri)
        dialog.show(childFragmentManager, "UploadDocumentDialog")
    }

    private fun downloadDocument(document: Document) {
        viewModel.downloadDocument(document)
    }

    private fun showEditDialog(document: Document) {
        val dialog = EditDocumentDialogFragment.newInstance(document)
        dialog.show(childFragmentManager, "EditDocumentDialog")
    }

    private fun confirmDelete(document: Document) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Documento")
            .setMessage("¿Estás seguro de que deseas enviar '${document.nombre}' a la papelera?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteDocument(document.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateToTrash() {
        try {
            // Usar Navigation Component
            findNavController().navigate(R.id.action_documentsFragment_to_deletedDocumentsFragment)
        } catch (e: Exception) {
            // Fallback: usar FragmentTransaction
            val fragment = DeletedDocumentsFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.navHostFragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}