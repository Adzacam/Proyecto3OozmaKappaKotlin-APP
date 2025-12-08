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
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.DialogEditDocumentBinding

class EditDocumentDialogFragment : DialogFragment() {

    private var _binding: DialogEditDocumentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocumentsViewModel by activityViewModels()
    private var document: Document? = null
    private var projects: List<Project> = emptyList()
    private var selectedFileUri: Uri? = null
    private var updateMode: UpdateMode = UpdateMode.KEEP_FILE

    enum class UpdateMode {
        KEEP_FILE,
        NEW_FILE,
        EXTERNAL_LINK
    }

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
        private const val ARG_DOCUMENT = "document"

        fun newInstance(document: Document): EditDocumentDialogFragment {
            return EditDocumentDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DOCUMENT, document as java.io.Serializable)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        arguments?.let {
            document = it.getSerializable(ARG_DOCUMENT) as? Document
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupListeners()
        populateFields()
    }

    private fun setupUI() {
        // Setup tipo de documento spinner
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
            binding.btnGuardar.isEnabled = !isLoading
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
                }, 300)
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

        // Pre-seleccionar el proyecto actual
        document?.let { doc ->
            val index = projects.indexOfFirst { it.id == doc.proyectoId }
            if (index != -1) {
                binding.spinnerProyecto.setSelection(index)
            }
        }
    }

    private fun populateFields() {
        document?.let { doc ->
            binding.etTitulo.setText(doc.nombre)
            binding.etDescripcion.setText(doc.descripcion)

            // Seleccionar tipo
            val typeIndex = DocumentType.values().indexOf(doc.tipo)
            if (typeIndex != -1) {
                binding.spinnerTipo.setSelection(typeIndex)
            }

            // Mostrar información del archivo actual
            if (doc.tipo == DocumentType.URL) {
                binding.tvArchivoActual.text = "Enlace: ${doc.enlaceExterno}"
                binding.radioEnlaceExterno.isChecked = true
                updateMode = UpdateMode.EXTERNAL_LINK
                binding.etEnlaceExterno.setText(doc.enlaceExterno)
            } else {
                binding.tvArchivoActual.text = "Archivo actual: ${doc.nombre}"
                binding.radioMantenerArchivo.isChecked = true
                updateMode = UpdateMode.KEEP_FILE
            }

            updateUIForMode()
        }
    }

    private fun setupListeners() {
        // Cancelar
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        // Guardar cambios
        binding.btnGuardar.setOnClickListener {
            updateDocument()
        }

        // Seleccionar nuevo archivo
        binding.btnSeleccionarArchivo.setOnClickListener {
            openFilePicker()
        }

        // Radio buttons para modo de actualización
        binding.radioMantenerArchivo.setOnClickListener {
            updateMode = UpdateMode.KEEP_FILE
            updateUIForMode()
        }

        binding.radioSubirNuevo.setOnClickListener {
            updateMode = UpdateMode.NEW_FILE
            updateUIForMode()
        }

        binding.radioEnlaceExterno.setOnClickListener {
            updateMode = UpdateMode.EXTERNAL_LINK
            updateUIForMode()
        }

        // Validación en tiempo real
        binding.etTitulo.doAfterTextChanged { validateForm() }
        binding.etEnlaceExterno.doAfterTextChanged { validateForm() }
    }

    private fun updateUIForMode() {
        when (updateMode) {
            UpdateMode.KEEP_FILE -> {
                binding.layoutNuevoArchivo.visibility = View.GONE
                binding.layoutEnlaceExterno.visibility = View.GONE
            }
            UpdateMode.NEW_FILE -> {
                binding.layoutNuevoArchivo.visibility = View.VISIBLE
                binding.layoutEnlaceExterno.visibility = View.GONE
            }
            UpdateMode.EXTERNAL_LINK -> {
                binding.layoutNuevoArchivo.visibility = View.GONE
                binding.layoutEnlaceExterno.visibility = View.VISIBLE
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

        // Obtener nombre del archivo
        val fileName = getFileName(uri)
        binding.tvNuevoArchivo.text = fileName

        // Validar tamaño
        val fileSize = getFileSize(uri)
        if (fileSize > 50 * 1024 * 1024) {
            binding.tvNuevoArchivo.text = "❌ Archivo muy grande (máx. 50MB)"
            binding.tvNuevoArchivo.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            selectedFileUri = null
        } else {
            val sizeMB = fileSize / (1024.0 * 1024.0)
            binding.tvNuevoArchivo.text = "✓ $fileName (${String.format("%.2f", sizeMB)} MB)"
            binding.tvNuevoArchivo.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
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
            updateMode == UpdateMode.EXTERNAL_LINK && enlaceExterno.isBlank() -> {
                binding.tilEnlaceExterno.error = "El enlace es requerido"
                false
            }
            updateMode == UpdateMode.EXTERNAL_LINK && !android.util.Patterns.WEB_URL.matcher(enlaceExterno).matches() -> {
                binding.tilEnlaceExterno.error = "Enlace inválido"
                false
            }
            updateMode == UpdateMode.NEW_FILE && selectedFileUri == null -> {
                Toast.makeText(requireContext(), "Selecciona un archivo", Toast.LENGTH_SHORT).show()
                false
            }
            else -> {
                binding.tilTitulo.error = null
                binding.tilEnlaceExterno.error = null
                true
            }
        }

        binding.btnGuardar.isEnabled = isValid
        return isValid
    }

    private fun updateDocument() {
        if (!validateForm() || document == null) return

        val selectedProject = if (binding.spinnerProyecto.selectedItemPosition >= 0) {
            projects[binding.spinnerProyecto.selectedItemPosition]
        } else null

        val titulo = binding.etTitulo.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim().ifBlank { null }
        val selectedType = DocumentType.values()[binding.spinnerTipo.selectedItemPosition]
        val enlaceExterno = binding.etEnlaceExterno.text.toString().trim().ifBlank { null }

        // Por ahora solo actualizamos metadatos
        // Para actualizar archivo necesitarías un endpoint diferente en tu API
        viewModel.updateDocument(
            id = document!!.id,
            nombre = titulo,
            descripcion = descripcion,
            projectId = selectedProject?.id,
            tipo = selectedType,
            enlaceExterno = if (updateMode == UpdateMode.EXTERNAL_LINK) enlaceExterno else null
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