package com.example.develarqapp.ui.documents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.develarqapp.databinding.FragmentDocumentsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.example.develarqapp.R

class DocumentsFragment : Fragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

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

        // Verificar acceso según rol
        if (!hasAccess()) {
            Toast.makeText(requireContext(), "No tienes acceso a esta sección", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupTopBar()
        setupUI()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        // Usa el acceso directo de ViewBinding, es más limpio y seguro
        topBarManager.setupTopBar(binding.topAppBar.root)
    }


    private fun setupUI() {
        // TODO: Implementar biblioteca de documentos
        // - Botón subir documento
        // - Botón ver papelera
        // - Filtros (tipo, proyecto, fecha)
        // - Lista de documentos con acciones (descargar, editar, eliminar)

        binding.tvPlaceholder.text = "Biblioteca de Documentos\n\n(Por implementar)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}