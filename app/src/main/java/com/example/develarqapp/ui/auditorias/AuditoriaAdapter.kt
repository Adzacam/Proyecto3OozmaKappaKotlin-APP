package com.example.develarqapp.ui.auditorias

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.AuditoriaLog
import com.example.develarqapp.databinding.ItemAuditoriaLogBinding

class AuditoriaAdapter : ListAdapter<AuditoriaLog, AuditoriaAdapter.AuditViewHolder>(AuditDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditViewHolder {
        val binding = ItemAuditoriaLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AuditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuditViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AuditViewHolder(
        private val binding: ItemAuditoriaLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: AuditoriaLog) {
            binding.apply {
                tvUsuario.text = log.usuario
                tvAccion.text = log.accion
                tvRegistro.text = log.registro
                tvFecha.text = log.fecha

                // Mostrar IP si está disponible
                if (!log.ip_address.isNullOrEmpty()) {
                    tvIpAddress.text = log.ip_address
                    tvIpAddress.isVisible = true
                } else {
                    tvIpAddress.isVisible = false
                }
            }
        }
    }

    class AuditDiffCallback : DiffUtil.ItemCallback<AuditoriaLog>() {
        override fun areItemsTheSame(oldItem: AuditoriaLog, newItem: AuditoriaLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AuditoriaLog, newItem: AuditoriaLog): Boolean {
            return oldItem == newItem
        }
    }
}