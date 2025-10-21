package com.example.develarqapp.ui.kanban

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.develarqapp.MainActivity
import com.example.develarqapp.databinding.FragmentKanbanBinding

class KanbanFragment : Fragment() {

    private var _binding: FragmentKanbanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKanbanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar menú hamburguesa
        binding.ivMenuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

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