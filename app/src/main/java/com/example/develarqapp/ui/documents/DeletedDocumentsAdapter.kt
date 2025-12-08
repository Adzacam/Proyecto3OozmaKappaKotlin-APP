package com.example.develarqapp.ui.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.databinding.ItemDeletedDocumentBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DeletedDocumentsAdapter(
    private val onRestoreClick: (Document) -> Unit,
    private val onPermanentDeleteClick: (Document) -> Unit
) : ListAdapter<Document, DeletedDocumentsAdapter.DeletedDocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedDocumentViewHolder {
        val binding = ItemDeletedDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeletedDocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeletedDocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeletedDocumentViewHolder(
        private val binding: ItemDeletedDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                // Título
                tvDocumentTitle.text = document.nombre

                // Descripción
                if (document.descripcion.isNullOrBlank()) {
                    tvDocumentDescription.visibility = android.view.View.GONE
                } else {
                    tvDocumentDescription.visibility = android.view.View.VISIBLE
                    tvDocumentDescription.text = document.descripcion
                }

                // Proyecto
                tvDocumentProject.text = document.proyectoNombre ?: "Proyecto #${document.proyectoId}"

                // Tipo
                val typeInfo = getTypeInfo(document.tipo)
                tvDocumentType.text = typeInfo.first
                tvDocumentType.setTextColor(typeInfo.second)

                // Fecha de eliminación
                document.fechaEliminacion?.let { fecha ->
                    val (formattedDate, daysAgo) = formatDeletedDate(fecha)
                    tvDeletedDate.text = "Eliminado: $formattedDate ($daysAgo días)"

                    // Advertencia si está próximo a ser purgado
                    if (daysAgo >= 25) {
                        tvWarning.visibility = android.view.View.VISIBLE
                        tvWarning.text = "⚠️ Se eliminará permanentemente en ${30 - daysAgo} días"
                    } else {
                        tvWarning.visibility = android.view.View.GONE
                    }
                } ?: run {
                    tvDeletedDate.text = "Fecha de eliminación desconocida"
                    tvWarning.visibility = android.view.View.GONE
                }

                // Botones
                btnRestore.setOnClickListener {
                    onRestoreClick(document)
                }

                btnPermanentDelete.setOnClickListener {
                    onPermanentDeleteClick(document)
                }
            }
        }

        private fun getTypeInfo(type: DocumentType): Pair<String, Int> {
            return when (type) {
                DocumentType.PDF -> Pair("PDF", android.graphics.Color.parseColor("#EF4444"))
                DocumentType.EXCEL -> Pair("Excel", android.graphics.Color.parseColor("#10B981"))
                DocumentType.WORD -> Pair("Word", android.graphics.Color.parseColor("#3B82F6"))
                DocumentType.URL -> Pair("URL", android.graphics.Color.parseColor("#8B5CF6"))
            }
        }

        private fun formatDeletedDate(dateString: String): Pair<String, Long> {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)

                val formatted = date?.let { outputFormat.format(it) } ?: dateString
                val daysAgo = date?.let {
                    val diff = System.currentTimeMillis() - it.time
                    TimeUnit.MILLISECONDS.toDays(diff)
                } ?: 0L

                Pair(formatted, daysAgo)
            } catch (_: Exception) {
                Pair(dateString, 0L)
            }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem == newItem
        }
    }
}