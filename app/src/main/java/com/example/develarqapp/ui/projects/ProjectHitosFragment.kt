package com.example.develarqapp.ui.projects

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.databinding.FragmentProjectHitosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProjectHitosFragment : Fragment() {

    private var _binding: FragmentProjectHitosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels()
    private val args: ProjectHitosFragmentArgs by navArgs()
    private lateinit var hitosAdapter: HitosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectHitosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupSwipeRefresh()
        setupUI()
        observeViewModel()

        // Cargar hitos
        viewModel.loadProjectHitos(args.projectId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvProjectTitle.text = args.projectName
    }

    private fun setupRecyclerView() {
        hitosAdapter = HitosAdapter(
            onEditClick = { hito ->
                showEditHitoDialog(hito)
            },
            onDeleteClick = { hito ->
                showDeleteDialog(hito)
            }
        )

        binding.rvHitos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hitosAdapter
        }
    }

    private fun setupFilters() {
        // Spinner de estado
        val estados = arrayOf("Todos", "Pendiente", "En Progreso", "Completado", "Bloqueado")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter

        binding.spinnerStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Búsqueda en tiempo real
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun applyFilters() {
        val estado = binding.spinnerStatus.selectedItem?.toString()?.let {
            if (it == "Todos") null else it
        }

        val search = binding.etSearch.text?.toString()

        viewModel.loadProjectHitos(args.projectId, estado)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProjectHitos(args.projectId)
        }
    }

    private fun setupUI() {
        binding.btnNewHito.setOnClickListener {
            showCreateHitoDialog()
        }
    }

    private fun showCreateHitoDialog() {
        val dialog = CreateHitoDialog()
        val bundle = Bundle().apply {
            putLong("project_id", args.projectId)
            putString("project_name", args.projectName)
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "CreateHitoDialog")
    }

    private fun showEditHitoDialog(hito: com.example.develarqapp.data.model.Hito) {
        val dialog = EditHitoDialog()
        val bundle = Bundle().apply {
            putLong("hito_id", hito.id)
            putString("hito_nombre", hito.nombre)
            putString("hito_descripcion", hito.descripcion)
            putString("hito_fecha", hito.fechaHito)
            putString("hito_estado", hito.estado.name)
            hito.encargadoId?.let { putLong("hito_encargado_id", it) }
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "EditHitoDialog")
    }

    private fun showDeleteDialog(hito: com.example.develarqapp.data.model.Hito) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Hito")
            .setMessage("¿Estás seguro de que deseas eliminar el hito '${hito.nombre}'?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteHito(hito.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.hitos.observe(viewLifecycleOwner) { hitos ->
            binding.swipeRefresh.isRefreshing = false

            if (hitos.isEmpty()) {
                binding.layoutEmptyState.isVisible = true
                binding.rvHitos.isVisible = false
            } else {
                binding.layoutEmptyState.isVisible = false
                binding.rvHitos.isVisible = true
                hitosAdapter.submitList(hitos)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.loadProjectHitos(args.projectId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}