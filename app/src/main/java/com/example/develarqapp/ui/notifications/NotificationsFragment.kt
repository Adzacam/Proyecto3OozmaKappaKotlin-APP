package com.example.develarqapp.ui.notifications

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.develarqapp.R
import com.example.develarqapp.databinding.FragmentNotificationsBinding
import com.example.develarqapp.utils.SessionManager
import com.example.develarqapp.utils.TopBarManager


class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var topBarManager: TopBarManager
    private lateinit var notificationsAdapter: NotificationsAdapter

    // MediaPlayer para el sonido de notificación
    private var notificationSound: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        topBarManager = TopBarManager(this, sessionManager)

        // Inicializar sonido de notificación
        notificationSound = MediaPlayer.create(requireContext(), R.raw.notification_sound)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        setupUI()
        setupRecyclerView()
        observeViewModel()

        // Cargar notificaciones
        viewModel.loadNotifications(sessionManager)
    }

    private fun setupTopBar() {
        topBarManager.setupTopBar(binding.topAppBar.root)
    }

    private fun setupUI() {
        // Botón volver al inicio
        binding.btnVolverInicio.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Botón marcar todas como leídas
        binding.btnMarcarTodasLeidas.setOnClickListener {
            viewModel.markAllAsRead(sessionManager)
        }

        // Botón mostrar filtros
        binding.btnMostrarFiltros.setOnClickListener {
            // TODO: Implementar filtros
            Toast.makeText(requireContext(), "Filtros - Por implementar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                if (!notification.isRead) {
                    viewModel.markAsRead(notification.id, sessionManager)
                }
                // TODO: Navegar al detalle según el tipo de notificación
                // Por ejemplo, si es de tipo "proyecto", navegar a ese proyecto
            },
            onMarkAsReadClick = { notification ->
                viewModel.markAsRead(notification.id, sessionManager)
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isEmpty()) {
                binding.tvEmptyState.isVisible = true
                binding.rvNotifications.isVisible = false
            } else {
                binding.tvEmptyState.isVisible = false
                binding.rvNotifications.isVisible = true
                notificationsAdapter.submitList(notifications)

                // Reproducir sonido si hay notificaciones nuevas
                if (notifications.any { it.isNew }) {
                    viewModel.playNotificationSound(notificationSound)
                }
            }
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            // Actualizar badge de notificaciones si es necesario
            // binding.ivNotificationIcon.isVisible = count > 0
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { success ->
            success?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationSound?.release()
        notificationSound = null
        _binding = null
    }
}