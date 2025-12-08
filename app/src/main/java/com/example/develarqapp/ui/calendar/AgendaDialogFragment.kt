package com.example.develarqapp.ui.calendar

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.DialogAgendaBinding
import com.example.develarqapp.utils.SessionManager

class AgendaDialogFragment : DialogFragment() {

    private var _binding: DialogAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by activityViewModels()
    private lateinit var agendaAdapter: MeetingsAdapter
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "AgendaDialogFragment"
    }

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
        sessionManager = SessionManager(requireContext())
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
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()

        val token = sessionManager.getToken()
        if (token != null) {
            Log.d(TAG, "🔄 Recargando reuniones en Agenda...")
            viewModel.loadMeetings(token)
        }
    }

    private fun setupRecyclerView() {
        agendaAdapter = MeetingsAdapter(
            onMeetingClick = { meeting ->
                Log.d(TAG, "Click en reunión: ${meeting.titulo}")
                dismiss()
                val editDialog = EditMeetingDialogFragment.newInstance(meeting)
                editDialog.show(parentFragmentManager, "EditMeetingDialog")
            },
            onDeleteClick = { meeting ->
                Log.d(TAG, "Solicitud de eliminar reunión: ${meeting.titulo}")
                showDeleteConfirmation(meeting)
            }
        )

        binding.recyclerViewAgenda.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = agendaAdapter
        }
    }

    private fun showDeleteConfirmation(meeting: com.example.develarqapp.data.model.Meeting) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Reunión")
            .setMessage("¿Estás seguro de eliminar la reunión '${meeting.titulo}'?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.deleteMeeting(meeting.id, token)
                    Log.d(TAG, "✅ Reunión eliminada: ${meeting.titulo}")
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error de sesión",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                resources.getColor(R.color.primaryColor, null),
                resources.getColor(android.R.color.holo_green_light, null),
                resources.getColor(android.R.color.holo_orange_light, null)
            )
            // Ajuste visual para que no choque con otros elementos
            setProgressViewOffset(false, 0, 150)

            setOnRefreshListener {
                val token = sessionManager.getToken()
                if (token != null) {
                    viewModel.loadMeetings(token)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.filteredMeetings.observe(viewLifecycleOwner) { meetings ->
            Log.d(TAG, "📅 Reuniones recibidas en Agenda: ${meetings.size}")
            binding.tvNoMeetings.isVisible = meetings.isEmpty()
            binding.recyclerViewAgenda.isVisible = meetings.isNotEmpty()
            agendaAdapter.submitList(meetings)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading

            if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }

            Log.d(TAG, "⏳ Loading: $isLoading")
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Reunión eliminada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.resetOperationSuccess()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(
                    requireContext(),
                    it,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.clearError()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            (parentFragment as? CalendarFragment)?.setViewModeToDay()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}