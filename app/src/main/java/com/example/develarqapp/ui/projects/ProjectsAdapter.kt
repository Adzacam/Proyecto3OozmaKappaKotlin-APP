package com.example.develarqapp.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.Project
import com.example.develarqapp.databinding.ItemProjectBinding
import com.example.develarqapp.utils.SimpleOnItemSelectedListener

class ProjectsAdapter(
    private val onEstadoChange: (Project, String) -> Unit
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
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentProject: Project? = null
        private val estadoListener = SimpleOnItemSelectedListener { position ->
            currentProject?.let { project ->
                val nuevoEstado = when(position) {
                    0 -> "activo"
                    1 -> "en progreso"
                    2 -> "finalizado"
                    else -> "activo"
                }
                
                // Solo notificar si realmente cambió
                if (nuevoEstado != project.estado) {
                    onEstadoChange(project, nuevoEstado)
                }
            }
        }

        init {
            binding.spinnerEstado.onItemSelectedListener = estadoListener
        }

        fun bind(project: Project) {
            currentProject = project
            
            binding.apply {
                // Nombre
                tvName.text = project.nombre
                
                // Cliente (desde relación o nombre directo)
                tvCliente.text = "Cliente: ${project.cliente?.name ?: "Sin cliente"}"
                
                // Responsable
                tvResponsable.text = "Responsable: ${project.responsable?.name ?: "—"}"
                
                // Descripción
                tvDescripcion.text = project.descripcion?.takeIf { it.isNotBlank() } 
                    ?: "Sin descripción"
                
                // Estado Spinner - resetear listener antes de setSelection
                estadoListener.reset()
                spinnerEstado.setSelection(
                    when(project.estado) {
                        "activo" -> 0
                        "en progreso" -> 1
                        "finalizado" -> 2
                        else -> 0
                    }
                )
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