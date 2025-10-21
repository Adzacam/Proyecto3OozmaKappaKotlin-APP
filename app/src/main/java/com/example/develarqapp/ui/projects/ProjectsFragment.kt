package com.example.develarqapp.ui.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.develarqapp.MainActivity
import com.example.develarqapp.databinding.FragmentProjectsBinding

class ProjectsFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar menú hamburguesa
        binding.ivMenuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        // Botón nuevo proyecto
        binding.btnNewProject.setOnClickListener {
            // TODO: Navegar a crear proyecto
        }

        // Configurar spinner de filtro
        setupFilterSpinner()
    }

    private fun setupFilterSpinner() {
        // TODO: Configurar opciones del spinner (Todos, Activo, En progreso, Finalizado)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

