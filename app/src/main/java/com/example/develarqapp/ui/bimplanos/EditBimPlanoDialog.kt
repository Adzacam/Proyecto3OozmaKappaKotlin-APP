package com.example.develarqapp.ui.bimplanos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import android.view.Window
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.DialogEditBimPlanoBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel

class EditBimPlanoDialog : DialogFragment() {

    private var _binding: DialogEditBimPlanoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels({ requireParentFragment() })
    private var planoId: Long = 0
    private var planoTitulo: String = ""
    private var planoDescripcion: String = ""

    companion object {
        fun newInstance(plano: BimPlano): EditBimPlanoDialog {
            val fragment = EditBimPlanoDialog()
            val args = Bundle().apply {
                putLong("plano_id", plano.id)
                putString("plano_titulo", plano.titulo)
                putString("plano_descripcion", plano.descripcion)
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
            planoDescripcion = it.getString("plano_descripcion") ?: ""
        }

        loadPlanoData()
        setupUI()
        observeViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditBimPlanoBinding.inflate(inflater, container, false)
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

    private fun loadPlanoData() {
        binding.etTituloPlano.setText(planoTitulo)
        binding.etDescripcion.setText(planoDescripcion)
    }

    private fun setupUI() {
        binding.btnGuardarCambios.setOnClickListener {
            validateAndUpdate()
        }

        binding.btnCancelar.setOnClickListener {
            dismiss()
        }
    }

    private fun validateAndUpdate() {
        val nuevoTitulo = binding.etTituloPlano.text?.toString()?.trim()
        val nuevaDescripcion = binding.etDescripcion.text?.toString()?.trim()

        if (nuevoTitulo.isNullOrBlank()) {
            Toast.makeText(requireContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardarCambios.isEnabled = false

        viewModel.updateBimPlano(
            id = planoId,
            nombre = nuevoTitulo,
            descripcion = nuevaDescripcion
        )
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
                binding.btnGuardarCambios.isEnabled = true
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}