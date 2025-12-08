package com.example.develarqapp.ui.bimplanos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.ItemBimPlanBinding

class BimPlanosAdapter(
    private val onDownloadClick: (BimPlano) -> Unit,
    private val onEditClick: (BimPlano) -> Unit,
    private val onDeleteClick: (BimPlano) -> Unit,
    private val onVersionsClick: (BimPlano) -> Unit
) : ListAdapter<BimPlano, BimPlanosAdapter.BimPlanViewHolder>(BimPlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BimPlanViewHolder {
        val binding = ItemBimPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BimPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BimPlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BimPlanViewHolder(
        private val binding: ItemBimPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: BimPlano) {
            binding.apply {
                tvTitle.text = plan.titulo
                tvProject.text = plan.proyectoNombre ?: "Sin proyecto"
                tvType.text = plan.tipo

                tvUrl.text = if (plan.archivoUrl.startsWith("http")) {
                    plan.archivoUrl.take(30) + "..."
                } else {
                    "Archivo local"
                }

                tvDate.text = formatearFecha(plan.fechaSubida)

                plan.version?.let {
                    tvVersion.text = "v$it"
                    tvVersion.visibility = View.VISIBLE
                } ?: run {
                    tvVersion.visibility = View.GONE
                }

                // ✅ Configurar listeners UNA SOLA VEZ
                btnDownload.setOnClickListener { onDownloadClick(plan) }
                btnEdit.setOnClickListener { onEditClick(plan) }
                btnDelete.setOnClickListener { onDeleteClick(plan) }
                btnVersions.setOnClickListener { onVersionsClick(plan) }
            }
        }


        private fun formatearFecha(fecha: String): String {
            return try {
                // Formato esperado: "2025-11-26 19:24:00"
                val parts = fecha.split(" ")
                val dateParts = parts[0].split("-")
                val timeParts = if (parts.size > 1) parts[1].split(":") else listOf("00", "00")

                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}\n${timeParts[0]}:${timeParts[1]}"
            } catch (e: Exception) {
                fecha
            }
        }
    }

    class BimPlanDiffCallback : DiffUtil.ItemCallback<BimPlano>() {
        override fun areItemsTheSame(oldItem: BimPlano, newItem: BimPlano): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BimPlano, newItem: BimPlano): Boolean {
            return oldItem == newItem
        }
    }
}