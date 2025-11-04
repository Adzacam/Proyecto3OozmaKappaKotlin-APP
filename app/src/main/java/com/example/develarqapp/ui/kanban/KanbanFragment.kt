package com.example.develarqapp.ui.kanban

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.develarqapp.databinding.FragmentKanbanBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.example.develarqapp.R

class KanbanFragment : Fragment() {

    private var _binding: FragmentKanbanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKanbanBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        setupUI()
    }

    private fun setupTopBar() {
        // Usa el acceso directo de ViewBinding, es más limpio y seguro
        topBarManager.setupTopBar(binding.topAppBar.root)
    }


    private fun setupUI() {
        // Configurar spinners
        setupSpinners()
    }

    private fun setupSpinners() {
        // TODO: Configurar spinner de proyectos
        // TODO: Configurar spinner de responsables
        // TODO: Cargar tareas cuando se seleccione un proyecto
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}