package com.example.develarqapp.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.ItemProjectBinding

class ProjectsAdapter(
    val onEditClick: (Project) -> Unit,
    val onVersionsClick: (Project) -> Unit,
    val onHitosClick: (Project) -> Unit,
    val onPermissionsClick: (Project) -> Unit
) : ListAdapter<Project, ProjectsAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.bind(project)
    }

    inner class ProjectViewHolder(
        val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.apply {

                // Información básica
                tvProjectName.text = project.nombre
                tvProjectDescription.text = project.descripcion ?: "Sin descripción"

                // Estado
                tvStatus.text = project.estado.replaceFirstChar { it.uppercase() }
                tvStatus.setBackgroundColor(getStatusColor(project.estado))

                // Datos simples
                tvClientName.text = "Cliente #${project.clienteId}"
                tvResponsableName.text = "Responsable #${project.responsableId}"

                // Fechas
                tvStartDate.text = "📅 Inicio: ${formatDate(project.fechaInicio)}"
                tvEndDate.text = if (project.fechaFin != null) {
                    "🏁 Fin: ${formatDate(project.fechaFin)}"
                } else {
                    "🏁 Sin fecha fin"
                }

                // BOTONES — AQUÍ ES DONDE SE CONECTA LA NAVEGACIÓN
                btnEdit.setOnClickListener { onEditClick(project) }
                btnVersions.setOnClickListener { onVersionsClick(project) }
                btnHitos.setOnClickListener { onHitosClick(project) }
                btnPermissions.setOnClickListener { onPermissionsClick(project) }
            }
        }

        private fun getStatusColor(estado: String): Int {
            return when (estado.lowercase()) {
                "activo" -> 0xFF10B981.toInt()
                "en progreso" -> 0xFFF59E0B.toInt()
                "finalizado" -> 0xFF64748B.toInt()
                else -> 0xFF3B82F6.toInt()
            }
        }

        private fun formatDate(date: String?): String {
            if (date.isNullOrEmpty()) return "N/A"
            return try {
                val parts = date.split("-")
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                date
            }
        }
    }

    class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem == newItem
        }
    }
}
