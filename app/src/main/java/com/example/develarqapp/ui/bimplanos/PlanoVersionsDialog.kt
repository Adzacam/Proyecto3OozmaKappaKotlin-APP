package com.example.develarqapp.ui.bimplanos

import android.view.Window
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.DialogPlanoVersionsBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlanoVersionsDialog : DialogFragment() {

    private var _binding: DialogPlanoVersionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels({ requireParentFragment() })
    private lateinit var versionsAdapter: PlanoVersionsAdapter
    private var planoId: Long = 0
    private var planoTitulo: String = ""

    companion object {
        fun newInstance(plano: BimPlano): PlanoVersionsDialog {
            val fragment = PlanoVersionsDialog()
            val args = Bundle().apply {
                putLong("plano_id", plano.id)
                putString("plano_titulo", plano.titulo)
            }
            fragment.arguments = args
            return fragment
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener argumentos
        arguments?.let {
            planoId = it.getLong("plano_id")
            planoTitulo = it.getString("plano_titulo") ?: ""
        }

        binding.tvTituloPlano.text = planoTitulo

        setupRecyclerView()
        setupUI()
        observeViewModel()

        // Cargar versiones
        viewModel.loadPlanoVersions(planoId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlanoVersionsBinding.inflate(inflater, container, false)
        // ✅ CORRECCIÓN 1
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRecyclerView() {
        versionsAdapter = PlanoVersionsAdapter(
            onSetCurrentClick = { version ->
                showSetCurrentDialog(version.id, version.version)
            },
            onDownloadClick = { version ->
                Toast.makeText(requireContext(), "Descargar v${version.version}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvVersions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = versionsAdapter
        }
    }

    private fun setupUI() {
        binding.btnCerrar.setOnClickListener {
            dismiss()
        }

        binding.btnSubirNuevaVersion.setOnClickListener {
            showUploadVersionDialog()
        }
    }

    private fun showUploadVersionDialog() {
        val plano = BimPlano(
            id = planoId,
            titulo = planoTitulo,
            descripcion = null,
            tipo = "",
            archivoUrl = "",
            version = null,
            fechaSubida = "",
            proyectoNombre = null,
            proyectoId = null,
            subidoPorNombre = null,
            eliminadoEl = null,
            diasRestantes = null
        )

        val dialog = UploadVersionDialog.newInstance(plano)
        dialog.show(childFragmentManager, "UploadVersionDialog")
    }

    private fun observeViewModel() {
        viewModel.planoVersions.observe(viewLifecycleOwner) { versions ->
            binding.progressBar.isVisible = false

            if (versions.isEmpty()) {
                binding.tvEmptyState.isVisible = true
                binding.rvVersions.isVisible = false
                binding.tvTotalVersiones.text = "No hay versiones disponibles"
                binding.tvEmptyState.text = "Este plano aún no tiene versiones registradas"
            } else {
                binding.tvEmptyState.isVisible = false
                binding.rvVersions.isVisible = true
                versionsAdapter.submitList(versions)
                binding.tvTotalVersiones.text = "Total de versiones: ${versions.size}"
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.progressBar.isVisible = false

                val mensajeError = when {
                    it.contains("no encontrado", ignoreCase = true) ->
                        "❌ El plano no existe o fue eliminado"
                    it.contains("token", ignoreCase = true) ->
                        "🔒 Sesión expirada. Inicia sesión nuevamente"
                    else ->
                        "⚠️ Error: $it"
                }

                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_LONG).show()
                viewModel.clearError()

                if (it.contains("no encontrado", ignoreCase = true)) {
                    dismiss()
                }
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), "✅ $it", Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
                viewModel.loadPlanoVersions(planoId)
            }
        }
    }

    private fun showSetCurrentDialog(versionId: Long, versionNumber: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Establecer Versión Actual")
            .setMessage("¿Deseas establecer la versión $versionNumber como la versión actual de este plano?")
            .setPositiveButton("Establecer") { _, _ ->
                viewModel.setVersionActual(versionId, planoId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}