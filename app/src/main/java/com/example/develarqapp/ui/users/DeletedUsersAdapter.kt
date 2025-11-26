package com.example.develarqapp.ui.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.ItemDeletedUserBinding

class DeletedUsersAdapter(
    private val onRestoreClick: (User) -> Unit
) : ListAdapter<User, DeletedUsersAdapter.DeletedUserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedUserViewHolder {
        val binding = ItemDeletedUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeletedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeletedUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeletedUserViewHolder(
        private val binding: ItemDeletedUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUserName.text = user.fullName
                tvUserEmail.text = user.email
                tvUserRole.text = user.rol?.replaceFirstChar { it.uppercaseChar() } ?: "Sin Rol"

                btnRestore.setOnClickListener {
                    onRestoreClick(user)
                }

            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}