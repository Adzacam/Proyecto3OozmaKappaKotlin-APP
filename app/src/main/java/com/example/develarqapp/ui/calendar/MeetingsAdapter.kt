package com.example.develarqapp.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.Meeting
import com.example.develarqapp.databinding.ItemMeetingBinding
import java.text.SimpleDateFormat
import java.util.*

class MeetingsAdapter(

    private val onMeetingClick: (Meeting) -> Unit,
    private val onDeleteClick: (Meeting) -> Unit
) : ListAdapter<Meeting, MeetingsAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding = ItemMeetingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MeetingViewHolder(
        private val binding: ItemMeetingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meeting: Meeting) {
            binding.apply {
                tvMeetingTitle.text = meeting.titulo
                tvMeetingProject.text = meeting.proyectoNombre ?: "Sin proyecto"

                // Formatear fecha y hora
                tvMeetingDate.text = formatDateTime(meeting.fechaHora)

                // Mostrar participantes
                val participantesText = if (meeting.participantes.isNullOrEmpty()) {
                    "Sin participantes"
                } else {
                    "${meeting.participantes.size} participante(s)"
                }
                tvMeetingParticipants.text = participantesText

                if (!meeting.descripcion.isNullOrEmpty()) {
                    tvMeetingDescription.text = meeting.descripcion
                } else tvMeetingDescription.text = "Sin descripción"
                root.setOnClickListener {
                    onMeetingClick(meeting)
                }

                btnDeleteMeeting.setOnClickListener {
                    onDeleteClick(meeting)
                }
            }
        }

        private fun formatDateTime(dateTimeStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateTimeStr)
                date?.let { outputFormat.format(it) } ?: dateTimeStr
            } catch (_: Exception) {
                dateTimeStr
            }
        }
    }

    class MeetingDiffCallback : DiffUtil.ItemCallback<Meeting>() {
        override fun areItemsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem == newItem
        }
    }
}