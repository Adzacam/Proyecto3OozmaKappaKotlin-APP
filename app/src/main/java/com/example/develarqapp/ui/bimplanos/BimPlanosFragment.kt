package com.example.develarqapp.ui.bimplanos

import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import androidx.navigation.fragment.findNavController
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.FragmentBimPlanosBinding
import com.example.develarqapp.ui.bimplans.BimPlanosViewModel
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class BimPlanosFragment : Fragment() {

    private var _binding: FragmentBimPlanosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BimPlanosViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var bimPlansAdapter: BimPlanosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBimPlanosBinding.inflate(inflater, container, false)
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
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // Cargar planos
        viewModel.loadBimPlanos()
    }

    private fun hasAccess(): Boolean {
        val role = sessionManager.getUserRol().lowercase()
        return role in listOf("admin", "ingeniero", "arquitecto")
    }

    private fun setupTopBar() {
        val topBarView = binding.root.findViewById<View>(com.example.develarqapp.R.id.topAppBar)
        topBarManager.setupTopBar(topBarView)
    }

    private fun setupUI() {
        // Botón subir plano
        binding.btnSubirPlano.setOnClickListener {
            showUploadDialog()
        }

        // Botón ver papelera
        binding.btnVerPapelera.setOnClickListener {
            navigateToPapelera()
        }

        // Botón limpiar filtros
        binding.btnLimpiarFiltros.setOnClickListener {
            clearFilters()
        }

        setupFilters()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                resources.getColor(R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_green_light, null),
                resources.getColor(android.R.color.holo_orange_light, null)
            )
            setProgressViewOffset(false, 0, 200)

            setOnRefreshListener {
                applyFilters() // Recarga aplicando los filtros actuales
            }
        }
    }

    // ============================================
    // MOSTRAR DIÁLOGO DE SUBIR PLANO
    // ============================================
    private fun showUploadDialog() {
        val dialog = UploadBimPlanoDialog()
        dialog.show(childFragmentManager, "UploadBimPlanoDialog")
    }
    // ============================================
    // MOSTRAR DIÁLOGO DE EDICIÓN
    // ============================================
    private fun showEditDialog(plano: BimPlano) {
        val dialog = EditBimPlanoDialog.newInstance(plano)
        dialog.show(childFragmentManager, "EditBimPlanoDialog")
    }

    // ============================================
    // MOSTRAR DIÁLOGO DE VERSIONES
    // ============================================
    private fun showVersionsDialog(plano: BimPlano) {
        val dialog = PlanoVersionsDialog.newInstance(plano)
        dialog.show(childFragmentManager, "PlanoVersionsDialog")
    }


    // ============================================
    // MOSTRAR DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ============================================
    private fun showDeleteDialog(plano: BimPlano) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Plano")
            .setMessage("¿Deseas mover '${plano.titulo}' a la papelera?\n\nPodrás restaurarlo dentro de 30 días.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteBimPlano(plano.id, "Eliminado desde la biblioteca")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============================================
    // NAVEGAR A PAPELERA
    // ============================================
    private fun navigateToPapelera() {
        try {
            findNavController().navigate(R.id.bimPlanosPapeleraFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al navegar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupFilters() {
        // Spinner de Tipo
        val tipos = arrayOf("Todos", "PDF", "GLB", "FBX", "Excel", "JPG/PNG")
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipo.adapter = tipoAdapter

        binding.spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Spinner de Orden
        val ordenOptions = arrayOf("Más recientes primero", "Más antiguos primero", "Nombre A-Z", "Nombre Z-A")
        val ordenAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ordenOptions)
        ordenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = ordenAdapter

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Date Pickers
        binding.etDateFrom.setOnClickListener { showDatePicker(true) }
        binding.etDateTo.setOnClickListener { showDatePicker(false) }

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
        val tipo = binding.spinnerTipo.selectedItem?.toString()?.let {
            if (it == "Todos") null else it
        }

        val search = binding.etSearch.text?.toString()?.takeIf { it.isNotBlank() }

        val fechaDesde = binding.etDateFrom.text?.toString()?.takeIf { it.isNotBlank() }
        val fechaHasta = binding.etDateTo.text?.toString()?.takeIf { it.isNotBlank() }

        val (orderBy, orderDir) = when (binding.spinnerSort.selectedItemPosition) {
            0 -> Pair("fecha_subida", "DESC") // Más recientes
            1 -> Pair("fecha_subida", "ASC")  // Más antiguos
            2 -> Pair("nombre", "ASC")        // A-Z
            3 -> Pair("nombre", "DESC")       // Z-A
            else -> Pair("fecha_subida", "DESC")
        }

        viewModel.loadBimPlanos(
            tipo = tipo,
            search = search,
            fechaDesde = fechaDesde,
            fechaHasta = fechaHasta,
            orderBy = orderBy,
            orderDir = orderDir
        )
    }

    private fun showDatePicker(isFromDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                if (isFromDate) {
                    binding.etDateFrom.setText(formattedDate)
                } else {
                    binding.etDateTo.setText(formattedDate)
                }
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun clearFilters() {
        binding.etSearch.text?.clear()
        binding.etDateFrom.text?.clear()
        binding.etDateTo.text?.clear()
        binding.spinnerTipo.setSelection(0)
        binding.spinnerSort.setSelection(0)
        viewModel.loadBimPlanos()
    }

    private fun setupRecyclerView() {
        bimPlansAdapter = BimPlanosAdapter(
            onDownloadClick = { plan ->
                downloadBimPlan(plan)
            },
            onEditClick = { plan ->
                Log.d("Fragment", "Abriendo diálogo editar para: ${plan.id}") // Debug
                showEditDialog(plan)
            },
            onDeleteClick = { plan ->
                showDeleteDialog(plan)
            },
            onVersionsClick = { plan ->
                showVersionsDialog(plan)
            }
        )

        binding.rvBimPlans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bimPlansAdapter
        }
    }

    // ============================================
    // DESCARGAR PLANO BIM
    // ============================================
    private fun downloadBimPlan(plano: BimPlano) {
        val downloadUrl = "${ApiConfig.BASE_URL}BimPlanos/download_bim_plano.php?id=${plano.id}"

        val extension = when {
            plano.tipo.equals("GLB", ignoreCase = true) -> "glb"
            plano.tipo.equals("FBX", ignoreCase = true) -> "fbx"
            plano.tipo.equals("PDF", ignoreCase = true) -> "pdf"
            plano.tipo.equals("Excel", ignoreCase = true) -> "xlsx"
            plano.tipo.equals("DWG", ignoreCase = true) -> "dwg"
            plano.tipo.equals("DXF", ignoreCase = true) -> "dxf"
            plano.tipo.equals("IFC", ignoreCase = true) -> "ifc"
            plano.tipo.contains("JPG", ignoreCase = true) ||
                    plano.tipo.contains("PNG", ignoreCase = true) -> {
                // Intentar detectar de la URL
                when {
                    plano.archivoUrl.endsWith(".jpg", ignoreCase = true) -> "jpg"
                    plano.archivoUrl.endsWith(".png", ignoreCase = true) -> "png"
                    else -> "jpg"
                }
            }
            else -> {
                // Último intento: extraer de la URL
                val urlExtension = plano.archivoUrl.substringAfterLast('.', "")
                if (urlExtension.isNotEmpty() && urlExtension.length <= 5) {
                    urlExtension
                } else {
                    "pdf" // Fallback
                }
            }
        }

        val fileName = "${plano.titulo}.$extension"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(plano.titulo)
            .setDescription("Descargando: $fileName")
            .addRequestHeader("Authorization", "Bearer ${sessionManager.getToken()}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(requireContext(), "📥 Descargando: $fileName", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewModel.bimPlanos.observe(viewLifecycleOwner) { plans ->
            binding.swipeRefresh.isRefreshing = false

            if (plans.isEmpty()) {
                binding.tvEmptyState.isVisible = true
                binding.rvBimPlans.isVisible = false
            } else {
                binding.tvEmptyState.isVisible = false
                binding.rvBimPlans.isVisible = true
                bimPlansAdapter.submitList(plans)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && !binding.swipeRefresh.isRefreshing) {
                binding.swipeRefresh.isRefreshing = true
            } else if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}