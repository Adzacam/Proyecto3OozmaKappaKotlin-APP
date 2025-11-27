package com.example.develarqapp.ui.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.DownloadHistory
import com.example.develarqapp.databinding.ItemDownloadHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class DownloadHistoryAdapter : ListAdapter<DownloadHistory, DownloadHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDownloadHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(download: DownloadHistory) {
            binding.apply {
                tvDocumentName.text = download.documentoNombre
                tvUserName.text = "Descargado por: ${download.userName}"
                tvProjectName.text = download.proyectoNombre ?: "Sin proyecto"

                try {
                    val date = inputFormat.parse(download.fechaDescarga)
                    tvDownloadDate.text = date?.let { outputFormat.format(it) }
                        ?: download.fechaDescarga
                } catch (e: Exception) {
                    tvDownloadDate.text = download.fechaDescarga
                }

                // Color según proyecto
                val color = when {
                    download.proyectoNombre == null -> R.color.text_gray
                    else -> R.color.primaryColor
                }
                root.setCardBackgroundColor(root.context.getColor(color))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadHistory>() {
        override fun areItemsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory) =
            oldItem == newItem
    }
}