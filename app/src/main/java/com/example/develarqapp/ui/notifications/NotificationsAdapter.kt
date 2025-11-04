package com.example.develarqapp.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.Notification
import com.example.develarqapp.databinding.ItemNotificationBinding

class NotificationsAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onMarkAsReadClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                // Tipo de notificación
                tvType.text = notification.type
                tvType.setBackgroundColor(getTypeColor(notification.type))

                // Badge "NUEVA" si corresponde
                tvNewBadge.isVisible = notification.isNew

                // Título y mensaje
                tvTitle.text = notification.title
                tvMessage.text = notification.message

                // Proyecto si existe
                if (notification.projectName != null) {
                    tvProjectName.isVisible = true
                    tvProjectName.text = notification.projectName
                } else {
                    tvProjectName.isVisible = false
                }

                // Fecha
                tvDate.text = notification.date

                // Indicador de no leído
                viewUnreadIndicator.isVisible = !notification.isRead

                // Botón marcar como leída
                btnMarkAsRead.apply {
                    text = if (notification.isRead) "LEÍDA" else "Marcar como leída"
                    isEnabled = !notification.isRead
                    setOnClickListener {
                        onMarkAsReadClick(notification)
                    }
                }

                // Click en toda la notificación
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }

        private fun getTypeColor(type: String): Int {
            return when (type.lowercase()) {
                "documento" -> android.graphics.Color.parseColor("#EC4899")
                "proyecto" -> android.graphics.Color.parseColor("#8B5CF6")
                else -> android.graphics.Color.parseColor("#6B7280")
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}