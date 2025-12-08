package com.example.develarqapp.ui.documents

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.R
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.DialogUploadDocumentBinding

class UploadDocumentDialogFragment : DialogFragment() {

    private var _binding: DialogUploadDocumentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentsViewModel by activityViewModels()

    private var selectedFileUri: Uri? = null
    private var projects: List<Project> = emptyList()
    private var preSelectedProjectId: Long? = null
    private var onDocumentUploadedListener: (() -> Unit)? = null

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    companion object {
        private const val ARG_PROJECT_ID = "project_id"
        private const val ARG_FILE_URI = "file_uri"

        fun newInstance(projectId: Long? = null, fileUri: Uri? = null): UploadDocumentDialogFragment {
            return UploadDocumentDialogFragment().apply {
                arguments = Bundle().apply {
                    projectId?.let { putLong(ARG_PROJECT_ID, it) }
                    fileUri?.let { putParcelable(ARG_FILE_URI, it) }
                }
            }
        }
        fun newInstance(
            projectId: Long? = null,
            fileUri: Uri? = null,
            onUploaded: (() -> Unit)? = null
        ): UploadDocumentDialogFragment {
            return UploadDocumentDialogFragment().apply {
                arguments = Bundle().apply {
                    projectId?.let { putLong(ARG_PROJECT_ID, it) }
                    fileUri?.let { putParcelable(ARG_FILE_URI, it) }
                }
                onDocumentUploadedListener = onUploaded
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        arguments?.let {
            preSelectedProjectId = if (it.containsKey(ARG_PROJECT_ID)) {
                it.getLong(ARG_PROJECT_ID)
            } else null
            selectedFileUri = it.getParcelable(ARG_FILE_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUploadDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupListeners()

        // Si ya hay un archivo seleccionado, mostrarlo
        selectedFileUri?.let { handleSelectedFile(it) }
    }

    private fun setupUI() {
        val types = DocumentType.values().map { it.displayName }
        val typeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_dark, types)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spinnerTipo.adapter = typeAdapter
    }

    private fun setupObservers() {
        // Observar proyectos
        viewModel.projects.observe(viewLifecycleOwner) { projectList ->
            projects = projectList
            setupProjectSpinner()
        }

        // Observar estados
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSubir.isEnabled = !isLoading
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty() && isVisible) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()

                onDocumentUploadedListener?.invoke()

                dismiss()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty() && isVisible) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                viewModel.loadDocuments()
                viewModel.clearMessages()

                binding.root.postDelayed({
                    if (isVisible) {
                        dismiss()
                    }
                }, 300) // 300ms de delay
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty() && isVisible) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                onDocumentUploadedListener?.invoke()
                viewModel.resetOperationSuccess()
                dismiss()
            }
        }

    }

    private fun setupProjectSpinner() {
        if (projects.isEmpty()) return

        val projectNames = projects.map { it.nombre }
        val projectAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_dark,
            projectNames
        )
        projectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spinnerProyecto.adapter = projectAdapter

        preSelectedProjectId?.let { projectId ->
            val index = projects.indexOfFirst { it.id == projectId }
            if (index != -1) {
                binding.spinnerProyecto.setSelection(index)
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnSubir.setOnClickListener {
            uploadDocument()
        }

        binding.btnSeleccionarArchivo.setOnClickListener {
            openFilePicker()
        }

        binding.spinnerTipo.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = DocumentType.values()[position]
                updateUIForDocumentType(selectedType)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.etTitulo.doAfterTextChanged { validateForm() }
        binding.etEnlaceExterno.doAfterTextChanged { validateForm() }
    }

    private fun updateUIForDocumentType(type: DocumentType) {
        when (type) {
            DocumentType.URL -> {
                binding.layoutEnlaceExterno.visibility = View.VISIBLE
                binding.layoutArchivo.visibility = View.GONE
                selectedFileUri = null
                binding.tvArchivoSeleccionado.text = "Ningún archivo seleccionado"
            }
            else -> {
                binding.layoutEnlaceExterno.visibility = View.GONE
                binding.layoutArchivo.visibility = View.VISIBLE
                binding.etEnlaceExterno.text?.clear()
            }
        }
        validateForm()
    }

    private fun openFilePicker() {
        val selectedType = DocumentType.values()[binding.spinnerTipo.selectedItemPosition]

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = when (selectedType) {
                DocumentType.PDF -> "application/pdf"
                DocumentType.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                DocumentType.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                DocumentType.URL -> "*/*"
            }
        }

        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri

        val fileName = getFileName(uri)
        binding.tvArchivoSeleccionado.text = fileName

        val fileSize = getFileSize(uri)
        if (fileSize > 50 * 1024 * 1024) {
            binding.tvArchivoSeleccionado.text = "❌ Archivo muy grande (máx. 50MB)"
            binding.tvArchivoSeleccionado.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            selectedFileUri = null
        } else {
            val sizeMB = fileSize / (1024.0 * 1024.0)
            binding.tvArchivoSeleccionado.text = "$fileName (${String.format("%.2f", sizeMB)} MB)"
            binding.tvArchivoSeleccionado.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

            if (binding.etTitulo.text.isNullOrBlank()) {
                binding.etTitulo.setText(fileName.substringBeforeLast("."))
            }
        }

        validateForm()
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "documento_${System.currentTimeMillis()}"

        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L

        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }

        return size
    }

    private fun validateForm(): Boolean {
        val titulo = binding.etTitulo.text.toString().trim()
        val selectedType = DocumentType.values()[binding.spinnerTipo.selectedItemPosition]
        val enlaceExterno = binding.etEnlaceExterno.text.toString().trim()

        val isValid = when {
            titulo.isBlank() -> {
                binding.tilTitulo.error = "El título es requerido"
                false
            }
            titulo.length > 150 -> {
                binding.tilTitulo.error = "El título no puede exceder 150 caracteres"
                false
            }
            selectedType == DocumentType.URL && enlaceExterno.isBlank() -> {
                binding.tilEnlaceExterno.error = "El enlace es requerido"
                false
            }
            selectedType == DocumentType.URL && !android.util.Patterns.WEB_URL.matcher(enlaceExterno).matches() -> {
                binding.tilEnlaceExterno.error = "Enlace inválido"
                false
            }
            selectedType != DocumentType.URL && selectedFileUri == null -> {
                Toast.makeText(requireContext(), "Selecciona un archivo", Toast.LENGTH_SHORT).show()
                false
            }
            else -> {
                binding.tilTitulo.error = null
                binding.tilEnlaceExterno.error = null
                true
            }
        }

        binding.btnSubir.isEnabled = isValid
        return isValid
    }

    private fun uploadDocument() {
        if (!validateForm()) return

        if (binding.spinnerProyecto.selectedItemPosition < 0 || projects.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedProject = projects[binding.spinnerProyecto.selectedItemPosition]
        val titulo = binding.etTitulo.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim().ifBlank { null }
        val selectedType = DocumentType.values()[binding.spinnerTipo.selectedItemPosition]
        val enlaceExterno = binding.etEnlaceExterno.text.toString().trim().ifBlank { null }

        viewModel.uploadDocument(
            projectId = selectedProject.id,
            nombre = titulo,
            descripcion = descripcion,
            tipo = selectedType,
            fileUri = selectedFileUri,
            enlaceExterno = enlaceExterno
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}