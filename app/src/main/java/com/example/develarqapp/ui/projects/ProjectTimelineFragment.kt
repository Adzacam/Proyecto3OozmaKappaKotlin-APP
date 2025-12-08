package com.example.develarqapp.ui.projects

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.example.develarqapp.databinding.FragmentProjectTimelineBinding
import java.util.Calendar

class ProjectTimelineFragment : Fragment() {

    private var _binding: FragmentProjectTimelineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectsViewModel by viewModels()
    private val args: ProjectTimelineFragmentArgs by navArgs()
    private lateinit var timelineAdapter: TimelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupSwipeRefresh()
        observeViewModel()

        // Cargar timeline
        viewModel.loadProjectTimeline(args.projectId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Mostrar nombre del proyecto
        binding.tvProjectTitle.text = args.projectName
    }

    private fun setupRecyclerView() {
        timelineAdapter = TimelineAdapter()

        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }
    }

    private fun setupFilters() {
        // Spinner de tipo de evento
        val eventTypes = arrayOf("Todos", "Proyecto", "Documento", "Reunión", "Tarea", "Hito", "Permiso", "Auditoría", "BIM")
        val eventAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypes)
        eventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = eventAdapter
        val users = arrayOf("Todos los usuarios")
        val userAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, users)
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUser.adapter = userAdapter

        // Date pickers
        binding.etDateFrom.setOnClickListener { showDatePicker(true) }
        binding.etDateTo.setOnClickListener { showDatePicker(false) }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProjectTimeline(args.projectId)
        }
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
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun observeViewModel() {
        viewModel.timeline.observe(viewLifecycleOwner) { events ->
            binding.swipeRefresh.isRefreshing = false

            if (events.isEmpty()) {
                binding.layoutEmptyState.isVisible = true
                binding.rvTimeline.isVisible = false
            } else {
                binding.layoutEmptyState.isVisible = false
                binding.rvTimeline.isVisible = true
                timelineAdapter.submitList(events)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}