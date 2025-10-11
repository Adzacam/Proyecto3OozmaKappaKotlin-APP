package com.example.develarqapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.develarqapp.MainActivity
import com.example.develarqapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón del menú para abrir el drawer
        binding.ivMenuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        // TODO: Configurar el nombre del usuario dinámicamente
        binding.tvUserName.text = "Adriano Leandro"
        binding.tvRole.text = "ingeniero"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
