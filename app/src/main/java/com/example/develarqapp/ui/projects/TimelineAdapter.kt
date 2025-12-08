package com.example.develarqapp.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.TimelineEvent
import com.example.develarqapp.data.model.TipoEvento
import com.example.develarqapp.databinding.ItemTimelineBinding

class TimelineAdapter : ListAdapter<TimelineEvent, TimelineAdapter.TimelineViewHolder>(TimelineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimelineViewHolder(
        private val binding: ItemTimelineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: TimelineEvent) {
            binding.apply {
                // Configurar según tipo de evento
                val (icon, color, typeText) = getEventTypeConfig(event.tipo)

                ivEventIcon.setImageResource(icon)
                ivEventIcon.setColorFilter(color)
                viewTimelineDot.setBackgroundColor(color)
                viewTimelineLine.setBackgroundColor(color)

                tvEventType.text = typeText
                tvEventType.setTextColor(color)

                // Información del evento
                tvEventTitle.text = event.titulo
                tvEventDescription.text = event.descripcion ?: "Sin descripción"
                tvEventUser.text = if (event.usuarioNombre != null) {
                    "Por: ${event.usuarioNombre}"
                } else {
                    "Usuario no especificado"
                }
                tvEventDate.text = formatDate(event.fecha)
            }
        }

        private fun getEventTypeConfig(tipo: TipoEvento): Triple<Int, Int, String> {
            val context = binding.root.context
            return when (tipo) {
                TipoEvento.PROYECTO -> Triple(
                    R.drawable.ic_projects,
                    ContextCompat.getColor(context, R.color.blue_500),
                    "PROYECTO"
                )
                TipoEvento.DOCUMENTO -> Triple(
                    R.drawable.ic_documents,
                    ContextCompat.getColor(context, R.color.yellow_500),
                    "DOCUMENTO"
                )
                TipoEvento.REUNION -> Triple(
                    R.drawable.ic_meetings,
                    ContextCompat.getColor(context, R.color.purple_500),
                    "REUNIÓN"
                )
                TipoEvento.TAREA -> Triple(
                    R.drawable.ic_task,
                    ContextCompat.getColor(context, R.color.green_500),
                    "TAREA"
                )
                TipoEvento.HITO -> Triple(
                    R.drawable.ic_milestone,
                    ContextCompat.getColor(context, R.color.pink_500),
                    "HITO"
                )
                TipoEvento.PERMISO -> Triple(
                    R.drawable.ic_lock,
                    ContextCompat.getColor(context, R.color.gray_500),
                    "PERMISO"
                )
                TipoEvento.AUDITORIA -> Triple(
                    R.drawable.ic_audit,
                    ContextCompat.getColor(context, R.color.red_500),
                    "AUDITORÍA"
                )
                TipoEvento.BIM -> Triple(
                    R.drawable.ic_bim,
                    ContextCompat.getColor(context, R.color.cyan_500),
                    "BIM"
                )
            }
        }

        private fun formatDate(fecha: String): String {
            return try {
                val parts = fecha.split(" ")
                val dateParts = parts[0].split("-")
                val timeParts = if (parts.size > 1) parts[1].split(":") else listOf("00", "00")

                "📅 ${dateParts[2]}/${dateParts[1]}/${dateParts[0]}, ${timeParts[0]}:${timeParts[1]}"
            } catch (e: Exception) {
                "📅 $fecha"
            }
        }
    }

    class TimelineDiffCallback : DiffUtil.ItemCallback<TimelineEvent>() {
        override fun areItemsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem == newItem
        }
    }
}