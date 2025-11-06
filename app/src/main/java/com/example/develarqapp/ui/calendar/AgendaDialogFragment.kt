package com.example.develarqapp.ui.calendar

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.databinding.DialogAgendaBinding

class AgendaDialogFragment : DialogFragment() {

    private var _binding: DialogAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by activityViewModels()
    private lateinit var agendaAdapter: MeetingsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        agendaAdapter = MeetingsAdapter(
            onMeetingClick = { meeting ->
                // Mostrar detalles
                (parentFragment as? CalendarFragment)?.let {
                    dismiss()
                }
            },
            onDeleteClick = { meeting ->
                // Eliminar reunión
            }
        )

        binding.recyclerViewAgenda.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = agendaAdapter
        }
    }

    private fun setupObservers() {
        viewModel.filteredMeetings.observe(viewLifecycleOwner) { meetings ->
            binding.tvNoMeetings.isVisible = meetings.isEmpty()
            binding.recyclerViewAgenda.isVisible = meetings.isNotEmpty()
            agendaAdapter.submitList(meetings)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}