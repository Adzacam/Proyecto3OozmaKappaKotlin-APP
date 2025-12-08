package com.example.develarqapp.ui.bimplanos

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.data.repository.ProjectRepository
import com.example.develarqapp.databinding.DialogUploadBimPlanoBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import kotlinx.coroutines.launch

class UploadBimPlanoDialog : DialogFragment() {

    private var _binding: DialogUploadBimPlanoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels({ requireParentFragment() })
    private lateinit var projectsRepository: ProjectRepository

    private var selectedFileUri: Uri? = null
    private var projects: List<Project> = emptyList()
    private var selectedProjectId: Long? = null

    // ============================================
    // FILE PICKER LAUNCHER
    // ============================================
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
            updateFileUI()
        }
    }

    // ============================================
    // LIFECYCLE
    // ============================================

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
        _binding = DialogUploadBimPlanoBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectsRepository = ProjectRepository(requireContext())

        setupUI()
        setupObservers()
        loadProjects()

    }

    // ============================================
    // SETUP UI
    // ============================================
    private fun setupUI() {
        // Spinner de Tipo de Archivo
        val tipos = arrayOf(
            "Plano en PDF",
            "Información Excel",
            "Imagen de Plano (JPG/PNG)",
            "Modelo FBX",
            "Modelo GLB"
        )
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipoArchivo.adapter = tipoAdapter

        // Botón seleccionar archivo
        binding.btnSeleccionarArchivo.setOnClickListener {
            openFilePicker()
        }

        // Botón subir
        binding.btnSubirPlano.setOnClickListener {
            validateAndUpload()
        }

        // Botón cancelar
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }
    }

    // ============================================
    // CARGAR PROYECTOS
    // ============================================
    private fun loadProjects() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = projectsRepository.getProjects()

                if (result.isSuccess) {
                    projects = result.getOrNull() ?: emptyList()
                    setupProjectSpinner()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar proyectos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupProjectSpinner() {
        val projectNames = projects.map { it.nombre }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            projectNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProyecto.adapter = adapter
    }

    // ============================================
    // FILE PICKER
    // ============================================
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "image/jpeg",
                "image/png",
                "model/gltf-binary", // .glb
                "application/octet-stream" // .fbx
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun updateFileUI() {
        selectedFileUri?.let { uri ->
            val fileName = getFileName(uri)
            binding.tvArchivoSeleccionado.text = fileName
            binding.tvArchivoSeleccionado.visibility = View.VISIBLE
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "archivo_seleccionado"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun setupObservers() {
        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSubirPlano.isEnabled = true
                viewModel.clearError()
            }
        }
    }
    // ============================================
    // VALIDAR Y SUBIR
    // ============================================
    private fun validateAndUpload() {
        val nombre = binding.etTituloPlano.text?.toString()?.trim()
        val descripcion = binding.etDescripcion.text?.toString()?.trim()
        val enlaceExterno = binding.etEnlaceExterno.text?.toString()?.trim()
        val projectPosition = binding.spinnerProyecto.selectedItemPosition

        // Validaciones
        if (nombre.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Ingresa un título", Toast.LENGTH_SHORT).show()
            return
        }

        if (projectPosition < 0 || projectPosition >= projects.size) {
            Toast.makeText(requireContext(), "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUri == null && enlaceExterno.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                "Selecciona un archivo o ingresa un enlace",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Obtener tipo de archivo
        val tipo = when (binding.spinnerTipoArchivo.selectedItemPosition) {
            0 -> "PDF"
            1 -> "Excel"
            2 -> "JPG/PNG"
            3 -> "FBX"
            4 -> "GLB"
            else -> "PDF"
        }

        // Obtener proyecto seleccionado
        selectedProjectId = projects[projectPosition].id

        // Subir
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubirPlano.isEnabled = false

        viewModel.uploadBimPlano(
            projectId = selectedProjectId!!,
            nombre = nombre,
            descripcion = descripcion,
            tipo = tipo,
            fileUri = selectedFileUri,
            enlaceExterno = enlaceExterno
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}