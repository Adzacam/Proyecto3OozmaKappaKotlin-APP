package com.example.develarqapp.ui.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.DownloadRecord
import com.example.develarqapp.databinding.ItemDownloadHistoryBinding
import java.text.SimpleDateFormat
import java.util.*


class DownloadHistoryAdapter : ListAdapter<DownloadRecord, DownloadHistoryAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(download: DownloadRecord) {
            binding.apply {

                tvDocumentName.text = download.documento
                tvUserName.text = "Descargado por: ${download.usuario}"
                tvProjectName.text = download.proyecto ?: "Sin proyecto"

                try {
                    val date = inputFormat.parse(download.fecha)
                    tvDownloadDate.text = date?.let { outputFormat.format(it) }
                        ?: download.fecha
                } catch (e: Exception) {
                    tvDownloadDate.text = download.fecha
                }

                // Color según si tiene proyecto o no (Estético opcional)
                val colorRes = if (download.proyecto == null) R.color.text_gray else R.color.card_background
                // Nota: Asegúrate de usar ContextCompat o una forma segura de obtener colores si esto falla
                // root.setCardBackgroundColor(ContextCompat.getColor(root.context, colorRes))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadRecord>() {
        override fun areItemsTheSame(oldItem: DownloadRecord, newItem: DownloadRecord) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadRecord, newItem: DownloadRecord) =
            oldItem == newItem
    }
}