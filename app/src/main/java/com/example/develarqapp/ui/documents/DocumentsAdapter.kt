package com.example.develarqapp.ui.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.Document
import com.example.develarqapp.data.model.DocumentType
import com.example.develarqapp.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.*

class DocumentsAdapter(
    private val onDownloadClick: (Document) -> Unit,
    private val onEditClick: (Document) -> Unit,
    private val onDeleteClick: (Document) -> Unit
) : ListAdapter<Document, DocumentsAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                // Título
                tvDocumentTitle.text = document.nombre

                // Descripción (opcional)
                if (document.descripcion.isNullOrBlank()) {
                    tvDocumentDescription.visibility = android.view.View.GONE
                } else {
                    tvDocumentDescription.visibility = android.view.View.VISIBLE
                    tvDocumentDescription.text = document.descripcion
                }

                // Proyecto
                tvDocumentProject.text = document.proyectoNombre ?: "Proyecto #${document.proyectoId}"

                // Tipo de documento
                val typeInfo = getTypeInfo(document.tipo)
                tvDocumentType.text = typeInfo.first
                tvDocumentType.setTextColor(typeInfo.second)

                // Fecha de subida
                tvDocumentDate.text = formatDate(document.fechaSubida)

                // Subido por
                tvDocumentUploadedBy.text = "Subido por: ${document.subidoPorNombre ?: "Desconocido"}"

                // Botones de acción
                btnDownload.setOnClickListener {
                    onDownloadClick(document)
                }

                btnEdit.setOnClickListener {
                    onEditClick(document)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(document)
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

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
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