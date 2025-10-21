package com.example.develarqapp.ui.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.ItemUserBinding

class UsersAdapter(
    private val onEditClick: (User) -> Unit,
    private val onToggleStatusClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                // Nombre completo
                tvUserName.text = user.fullName

                // Email
                tvUserEmail.text = user.email

                // Rol
                tvUserRole.text = user.rol.replaceFirstChar { it.uppercase() }

                // Estado
                tvUserStatus.text = user.estado.replaceFirstChar { it.uppercase() }

                // Cambiar color según estado
                val statusColor = if (user.estado == "activo") {
                    android.graphics.Color.parseColor("#10B981")
                } else {
                    android.graphics.Color.parseColor("#EF4444")
                }
                tvUserStatus.setTextColor(statusColor)

                // Botón editar
                btnEdit.setOnClickListener {
                    onEditClick(user)
                }

                // Long click para más opciones
                root.setOnLongClickListener {
                    showOptionsMenu(user)
                    true
                }
            }
        }

        private fun showOptionsMenu(user: User) {
            val context = binding.root.context
            val popup = androidx.appcompat.widget.PopupMenu(context, binding.root)

            popup.menu.add(0, 1, 0, "Editar")
            popup.menu.add(0, 2, 0, if (user.estado == "activo") "Desactivar" else "Activar")
            popup.menu.add(0, 3, 0, "Eliminar")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        onEditClick(user)
                        true
                    }
                    2 -> {
                        onToggleStatusClick(user)
                        true
                    }
                    3 -> {
                        onDeleteClick(user)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
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