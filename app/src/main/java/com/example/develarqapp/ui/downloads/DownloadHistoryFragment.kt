package com.example.develarqapp.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.develarqapp.databinding.FragmentDownloadHistoryBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.example.develarqapp.R

class DownloadHistoryFragment : Fragment() {

    private var _binding: FragmentDownloadHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadHistoryBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar acceso - solo admin
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
        return role == "admin"
    }

    private fun setupTopBar() {
        // Usa el acceso directo de ViewBinding, es más limpio y seguro
        topBarManager.setupTopBar(binding.topAppBar.root)
    }


    private fun setupUI() {
        // TODO: Implementar historial de descargas
        // - Mostrar todas las descargas del sistema
        // - Filtros por usuario, fecha, tipo de archivo
        // - Información: quién descargó qué y cuándo

        binding.tvPlaceholder.text = "Historial Global de Descargas (Admin)\n\n(Por implementar)"
        binding.tvEmptyMessage.text = "No se ha registrado ninguna descarga en el sistema"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}