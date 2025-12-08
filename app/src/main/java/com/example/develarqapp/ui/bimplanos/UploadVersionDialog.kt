package com.example.develarqapp.ui.bimplanos

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.develarqapp.R
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.DialogUploadVersionBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UploadVersionDialog : DialogFragment() {

    private var _binding: DialogUploadVersionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels({ requireParentFragment() })
    private var selectedFileUri: Uri? = null
    private var planoOriginalId: Long = 0
    private var planoTitulo: String = ""

    companion object {
        private const val ARG_PLANO_ID = "plano_id"
        private const val ARG_PLANO_TITULO = "plano_titulo"

        fun newInstance(plano: BimPlano): UploadVersionDialog {
            return UploadVersionDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PLANO_ID, plano.id)
                    putString(ARG_PLANO_TITULO, plano.titulo)
                }
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
            updateFileUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUploadVersionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            planoOriginalId = it.getLong(ARG_PLANO_ID)
            planoTitulo = it.getString(ARG_PLANO_TITULO, "")
        }

        binding.tvPlanoOriginal.text = "Nueva versión de: $planoTitulo"
        binding.etTituloPlano.setText(planoTitulo) // ✅ Pre-llenar con el título original

        setupUI()
        observeViewModel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .create()
    }

    private fun setupUI() {
        binding.btnSeleccionarArchivo.setOnClickListener {
            openFilePicker()
        }

        binding.btnSubirVersion.setOnClickListener {
            validateAndUpload()
        }

        binding.btnCancelar.setOnClickListener {
            dismiss()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "model/gltf-binary",
                "application/octet-stream",
                "image/jpeg",
                "image/png",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
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

    private fun validateAndUpload() {
        val titulo = binding.etTituloPlano.text?.toString()?.trim()
        val descripcion = binding.etDescripcion.text?.toString()?.trim()

        if (titulo.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Ingresa un título", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Selecciona un archivo", Toast.LENGTH_SHORT).show()
            return
        }

        val tipo = detectarTipoArchivo(selectedFileUri!!)

        android.util.Log.d("UploadVersionDialog", "📤 Subiendo: titulo='$titulo', tipo='$tipo'")

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubirVersion.isEnabled = false

        viewModel.uploadNewVersion(
            planoOriginalId = planoOriginalId,
            nombre = titulo,
            descripcion = descripcion,
            tipo = tipo,
            fileUri = selectedFileUri
        )
    }

    private fun detectarTipoArchivo(uri: Uri): String {
        val mimeType = requireContext().contentResolver.getType(uri)
        return when {
            mimeType?.contains("pdf") == true -> "PDF"
            mimeType?.contains("excel") == true || mimeType?.contains("spreadsheet") == true -> "Excel"
            mimeType?.contains("image") == true -> "JPG/PNG"
            getFileName(uri).endsWith(".glb", ignoreCase = true) -> "GLB"
            getFileName(uri).endsWith(".fbx", ignoreCase = true) -> "FBX"
            else -> "PDF"
        }
    }

    private fun observeViewModel() {
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
                binding.btnSubirVersion.isEnabled = true
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}