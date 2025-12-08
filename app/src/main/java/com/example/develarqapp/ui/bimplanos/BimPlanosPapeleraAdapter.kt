package com.example.develarqapp.ui.bimplanos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.BimPlano
import com.example.develarqapp.databinding.ItemBimPlanoPapeleraBinding

class BimPlanosPapeleraAdapter(
    private val onRestoreClick: (BimPlano) -> Unit,
    private val onDeleteClick: (BimPlano) -> Unit
) : ListAdapter<BimPlano, BimPlanosPapeleraAdapter.PapeleraViewHolder>(PapaleraDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PapeleraViewHolder {
        val binding = ItemBimPlanoPapeleraBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PapeleraViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PapeleraViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PapeleraViewHolder(
        private val binding: ItemBimPlanoPapeleraBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plano: BimPlano) {
            binding.apply {
                tvTitulo.text = plano.titulo
                tvProyecto.text = plano.proyectoNombre ?: "Sin proyecto"
                tvTipo.text = plano.tipo

                // Formatear fecha de eliminación
                plano.eliminadoEl?.let {
                    tvEliminadoEl.text = "Eliminado: ${formatearFecha(it)}"
                }

                // Mostrar días restantes
                plano.diasRestantes?.let { dias ->
                    when {
                        dias > 7 -> {
                            tvDiasRestantes.text = "Se eliminará en $dias días"
                            tvDiasRestantes.setTextColor(0xFF94A3B8.toInt()) // Gris
                        }
                        dias in 1..7 -> {
                            tvDiasRestantes.text = "⚠️ Se eliminará en $dias días"
                            tvDiasRestantes.setTextColor(0xFFFBBF24.toInt()) // Amarillo
                        }
                        else -> {
                            tvDiasRestantes.text = "🔴 Se eliminará hoy"
                            tvDiasRestantes.setTextColor(0xFFEF4444.toInt()) // Rojo
                        }
                    }
                }

                btnRestaurar.setOnClickListener { onRestoreClick(plano) }
                btnEliminar.setOnClickListener { onDeleteClick(plano) }
            }
        }

        private fun formatearFecha(fecha: String): String {
            return try {
                val parts = fecha.split(" ")
                val dateParts = parts[0].split("-")
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
            } catch (e: Exception) {
                fecha
            }
        }
    }

    class PapaleraDiffCallback : DiffUtil.ItemCallback<BimPlano>() {
        override fun areItemsTheSame(oldItem: BimPlano, newItem: BimPlano): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BimPlano, newItem: BimPlano): Boolean {
            return oldItem == newItem
        }
    }
}