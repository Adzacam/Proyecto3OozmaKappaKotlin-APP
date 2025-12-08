package com.example.develarqapp.ui.bimplanos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.PlanoVersion
import com.example.develarqapp.databinding.ItemPlanoVersionBinding

class PlanoVersionsAdapter(
    private val onSetCurrentClick: (PlanoVersion) -> Unit,
    private val onDownloadClick: (PlanoVersion) -> Unit
) : ListAdapter<PlanoVersion, PlanoVersionsAdapter.VersionViewHolder>(VersionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VersionViewHolder {
        val binding = ItemPlanoVersionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VersionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VersionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VersionViewHolder(
        private val binding: ItemPlanoVersionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(version: PlanoVersion) {
            binding.apply {
                // Información básica
                tvVersion.text = "Versión ${version.version}"
                tvDescripcion.text = version.descripcion ?: "Sin descripción"
                tvSubidoPor.text = "Subido por: ${version.subidoPorNombre ?: "Desconocido"}"
                tvFecha.text = formatearFecha(version.fechaSubida)

                // Badge de versión actual
                tvVersionActual.isVisible = version.esVersionActual

                // Botones
                btnEstablecerActual.isVisible = !version.esVersionActual
                btnEstablecerActual.setOnClickListener { onSetCurrentClick(version) }
                btnDescargar.setOnClickListener { onDownloadClick(version) }
            }
        }

        private fun formatearFecha(fecha: String): String {
            return try {
                val parts = fecha.split(" ")
                val dateParts = parts[0].split("-")
                val timeParts = if (parts.size > 1) parts[1].split(":") else listOf("00", "00")

                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
            } catch (e: Exception) {
                fecha
            }
        }
    }

    class VersionDiffCallback : DiffUtil.ItemCallback<PlanoVersion>() {
        override fun areItemsTheSame(oldItem: PlanoVersion, newItem: PlanoVersion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PlanoVersion, newItem: PlanoVersion): Boolean {
            return oldItem == newItem
        }
    }
}