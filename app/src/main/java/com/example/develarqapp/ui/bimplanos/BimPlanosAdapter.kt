package com.example.develarqapp.ui.bimplans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.example.develarqapp.data.model.BimPlanos
import com.example.develarqapp.databinding.ItemBimPlanBinding

class BimPlanosAdapter(
    private val onDownloadClick: (BimPlanos) -> Unit,
    private val onEditClick: (BimPlanos) -> Unit,
    private val onDeleteClick: (BimPlanos) -> Unit
) : ListAdapter<BimPlanos, BimPlanosAdapter.BimPlanViewHolder>(BimPlanDiffCallback()) {

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

        fun bind(plan: BimPlanos) {
            binding.apply {
                tvTitle.text = plan.title
                tvProject.text = plan.project
                tvType.text = plan.type
                tvUrl.text = plan.url
                tvDate.text = plan.date

                btnDownload.setOnClickListener { onDownloadClick(plan) }
                btnEdit.setOnClickListener { onEditClick(plan) }
                btnDelete.setOnClickListener { onDeleteClick(plan) }
            }
        }
    }

    class BimPlanDiffCallback : DiffUtil.ItemCallback<BimPlanos>() {
        override fun areItemsTheSame(oldItem: BimPlanos, newItem: BimPlanos): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BimPlanos, newItem: BimPlanos): Boolean {
            return oldItem == newItem
        }
    }
}