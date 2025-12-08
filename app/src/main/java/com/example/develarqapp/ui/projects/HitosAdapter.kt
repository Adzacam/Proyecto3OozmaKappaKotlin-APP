package com.example.develarqapp.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.EstadoHito
import com.example.develarqapp.data.model.Hito
import com.example.develarqapp.databinding.ItemHitoBinding

class HitosAdapter(
    private val onEditClick: (Hito) -> Unit,
    private val onDeleteClick: (Hito) -> Unit
) : ListAdapter<Hito, HitosAdapter.HitoViewHolder>(HitoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HitoViewHolder {
        val binding = ItemHitoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HitoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HitoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HitoViewHolder(
        private val binding: ItemHitoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hito: Hito) {
            binding.apply {
                // Información básica
                tvHitoName.text = hito.nombre
                tvHitoDescription.text = hito.descripcion ?: "Sin descripción"
                tvHitoDate.text = "📅 ${formatDate(hito.fechaHito)}"

                // Encargado
                tvHitoEncargado.text = if (hito.encargadoNombre != null) {
                    "👤 Responsable: ${hito.encargadoNombre}"
                } else {
                    "👤 Sin responsable asignado"
                }

                // Estado
                val (icon, color, statusText) = getStatusConfig(hito.estado)
                ivStatusIcon.setImageResource(icon)
                cardStatusIcon.setCardBackgroundColor(color)
                tvStatus.text = statusText
                tvStatus.setTextColor(color)

                // Botones
                btnEdit.setOnClickListener { onEditClick(hito) }
                btnDelete.setOnClickListener { onDeleteClick(hito) }
            }
        }

        private fun getStatusConfig(estado: EstadoHito): Triple<Int, Int, String> {
            val context = binding.root.context
            return when (estado) {
                EstadoHito.PENDIENTE -> Triple(
                    R.drawable.ic_pending,
                    ContextCompat.getColor(context, R.color.gray_500),
                    "Pendiente"
                )
                EstadoHito.EN_PROGRESO -> Triple(
                    R.drawable.ic_progress,
                    ContextCompat.getColor(context, R.color.yellow_500),
                    "En Progreso"
                )
                EstadoHito.COMPLETADO -> Triple(
                    R.drawable.ic_check,
                    ContextCompat.getColor(context, R.color.green_500),
                    "Completado"
                )
                EstadoHito.BLOQUEADO -> Triple(
                    R.drawable.ic_block,
                    ContextCompat.getColor(context, R.color.red_500),
                    "Bloqueado"
                )
            }
        }

        private fun formatDate(date: String): String {
            return try {
                val parts = date.split("-")
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                date
            }
        }
    }

    class HitoDiffCallback : DiffUtil.ItemCallback<Hito>() {
        override fun areItemsTheSame(oldItem: Hito, newItem: Hito): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hito, newItem: Hito): Boolean {
            return oldItem == newItem
        }
    }
}