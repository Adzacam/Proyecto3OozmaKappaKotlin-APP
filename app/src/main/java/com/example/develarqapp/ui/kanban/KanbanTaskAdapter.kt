package com.example.develarqapp.ui.kanban

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.TaskComplete
import com.example.develarqapp.databinding.ItemKanbanTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class KanbanTaskAdapter(
    private val onTaskClick: (TaskComplete) -> Unit,
    private val onTaskLongClick: (TaskComplete) -> Boolean
) : ListAdapter<TaskComplete, KanbanTaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemKanbanTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemKanbanTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskLongClick(getItem(position))
                } else {
                    false
                }
            }
        }

        fun bind(task: TaskComplete) {
            with(binding) {
                // Título de la tarea
                tvTaskTitle.text = task.titulo

                // Descripción
                if (!task.descripcion.isNullOrEmpty()) {
                    tvTaskDescription.text = task.descripcion
                    tvTaskDescription.visibility = View.VISIBLE
                } else {
                    tvTaskDescription.visibility = View.GONE
                }

                // Prioridad
                tvPriority.text = task.prioridad.uppercase()
                tvPriority.setTextColor(task.prioridadColor)
                tvPriority.setBackgroundColor(
                    adjustAlpha(task.prioridadColor, 0.2f)
                )

                // Asignado
                if (!task.asignadoNombre.isNullOrEmpty()) {
                    tvAssignedTo.text = "👤 ${task.asignadoNombre}"
                    tvAssignedTo.visibility = View.VISIBLE
                } else {
                    tvAssignedTo.text = "Sin asignar"
                    tvAssignedTo.visibility = View.VISIBLE
                }

                // Fecha límite
                if (!task.fechaLimite.isNullOrEmpty()) {
                    tvDueDate.text = "📅 ${formatDate(task.fechaLimite)}"
                    tvDueDate.visibility = View.VISIBLE

                    // Marcar si está vencida
                    if (isOverdue(task.fechaLimite) && task.estado != "completado") {
                        tvDueDate.setTextColor(
                            root.context.getColor(R.color.error)
                        )
                    } else {
                        tvDueDate.setTextColor(
                            root.context.getColor(R.color.text_dark_gray)
                        )
                    }
                } else {
                    tvDueDate.visibility = View.GONE
                }
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }

        private fun isOverdue(dateString: String): Boolean {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dueDate = format.parse(dateString)
                val today = Calendar.getInstance().time
                dueDate?.before(today) == true
            } catch (e: Exception) {
                false
            }
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
            val red = android.graphics.Color.red(color)
            val green = android.graphics.Color.green(color)
            val blue = android.graphics.Color.blue(color)
            return android.graphics.Color.argb(alpha, red, green, blue)
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskComplete>() {
        override fun areItemsTheSame(oldItem: TaskComplete, newItem: TaskComplete): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskComplete, newItem: TaskComplete): Boolean {
            return oldItem == newItem
        }
    }
}