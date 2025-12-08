package com.example.develarqapp.ui.auditorias

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
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

        private var isExpanded = false

        fun bind(log: AuditoriaLog) {
            binding.apply {
                //Usuario
                tvUsuario.text = log.usuario

                //Fecha
                tvFecha.text = log.fecha

                //Acción principal (título)
                tvAccion.text = log.accion

                // 🏷Tipo de tabla
                val nombreTabla = getTablaNombre(log.tablaAfectada)
                tvTipo.text = nombreTabla
                tvTipo.isVisible = !log.tablaAfectada.isNullOrEmpty()

                // Ícono y color según tabla
                val iconInfo = getIconForTable(log.tablaAfectada)
                ivIcon.setImageResource(iconInfo.first)
                ivIcon.setColorFilter(
                    ContextCompat.getColor(binding.root.context, iconInfo.second)
                )

                //Nombre del recurso (extraído de la acción)
                val nombreRecurso = extractResourceName(log.accion) ?: "ID: ${log.registro}"
                tvRegistro.text = nombreRecurso

                // 🌐 IP Address (mostrar en una línea)
                if (!log.ip_address.isNullOrEmpty()) {
                    tvIpAddress.text = "🌐 ${log.ip_address}"
                    tvIpAddress.isVisible = true
                } else {
                    tvIpAddress.isVisible = false
                }

                // Configurar descripción detallada (expandible)
                setupDescripcion(log)
            }
        }

        /**
         *  Configura la descripción detallada con formato legible
         */
        private fun setupDescripcion(log: AuditoriaLog) {
            binding.apply {
                if (!log.descripcionDetallada.isNullOrEmpty()) {
                    // Formatear la descripción
                    val formatted = formatDescription(log.descripcionDetallada)
                    tvDescripcionDetallada.text = formatted

                    // MOSTRAR el CardView (estaba en gone por defecto)
                    cvDescripcion.isVisible = isExpanded

                    // Mostrar hint cuando está colapsado
                    tvExpandHint.isVisible = !isExpanded

                    // Lógica de expansión al tocar la tarjeta
                    root.setOnClickListener {
                        isExpanded = !isExpanded
                        cvDescripcion.isVisible = isExpanded
                        tvExpandHint.isVisible = !isExpanded
                    }

                    root.isClickable = true
                    root.isFocusable = true

                } else {
                    // Si no hay descripción, ocultar
                    cvDescripcion.isVisible = false
                    tvExpandHint.isVisible = false
                    root.setOnClickListener(null)
                    root.isClickable = false
                }
            }
        }

        /**
         *Formatea la descripción detallada con secciones claras
         */
        private fun formatDescription(fullDesc: String): String {
            val sections = mutableListOf<String>()

            // Buscar secciones por palabras clave
            val lines = fullDesc.split("\n")
            val result = StringBuilder()

            for (line in lines) {
                when {
                    line.contains("ACCIÓN REALIZADA:", ignoreCase = true) -> {
                        result.append("\n📌 DETALLES:\n")
                    }
                    line.contains("JUSTIFICACIÓN:", ignoreCase = true) -> {
                        result.append("\n⚠️ JUSTIFICACIÓN:\n")
                    }
                    line.contains("INFORMACIÓN DEL DISPOSITIVO:", ignoreCase = true) ||
                            line.contains("Información del dispositivo:", ignoreCase = true) -> {
                        result.append("\n📱 INFORMACIÓN DEL DISPOSITIVO:\n")
                    }
                    line.contains("DETALLES:", ignoreCase = true) -> {
                        result.append("📌 DETALLES:\n")
                    }
                    line.isNotBlank() -> {
                        result.append(line).append("\n")
                    }
                }
            }

            return result.toString().trim()
        }

        /**
         * Extrae el nombre del recurso de la acción
         * Ejemplo: "Eliminó el usuario 'Juan Pérez'" -> 'Juan Pérez'
         */
        private fun extractResourceName(accion: String): String? {
            val regex = "'([^']+)'".toRegex()
            return regex.find(accion)?.value
        }

        /**
         *Obtiene ícono y color según la tabla afectada
         */
        private fun getIconForTable(tabla: String?): Pair<Int, Int> {
            return when (tabla?.lowercase()) {
                "users" -> Pair(R.drawable.ic_employees, R.color.iconBlue)
                "proyectos" -> Pair(R.drawable.ic_projects, R.color.iconGreen)
                "documentos" -> Pair(R.drawable.ic_documents, R.color.iconYellow)
                "reuniones" -> Pair(R.drawable.ic_calendar, R.color.iconPurple)
                "tareas" -> Pair(R.drawable.ic_kanban, R.color.iconOrange)
                else -> Pair(R.drawable.ic_audit, R.color.iconGray)
            }
        }

        /**
         *  Obtiene el nombre legible de la tabla
         */
        private fun getTablaNombre(tabla: String?): String {
            return when (tabla?.lowercase()) {
                "users" -> "👥 Usuarios"
                "proyectos" -> "📁 Proyectos"
                "documentos" -> "📄 Documentos"
                "reuniones" -> "📅 Reuniones"
                "tareas" -> "✅ Tareas"
                else -> tabla?.replaceFirstChar { it.uppercase() } ?: "General"
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