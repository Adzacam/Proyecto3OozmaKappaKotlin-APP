package com.example.develarqapp.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.develarqapp.R
import com.example.develarqapp.data.model.ProjectPermission
import com.example.develarqapp.databinding.ItemPermissionBinding

class PermissionsAdapter : ListAdapter<ProjectPermission, PermissionsAdapter.PermissionViewHolder>(PermissionDiffCallback()) {

    private val permissions = mutableMapOf<Long, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val binding = ItemPermissionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PermissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getUpdatedPermissions(): Map<Long, String> = permissions

    inner class PermissionViewHolder(
        private val binding: ItemPermissionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(permission: ProjectPermission) {
            binding.apply {
                // Nombre del usuario
                tvUserName.text = permission.userNombre

                // Rol en proyecto (si existe)
                if (permission.rolEnProyecto != null) {
                    tvRolEnProyecto.text = "Rol: ${permission.rolEnProyecto}"
                    tvRolEnProyecto.visibility = android.view.View.VISIBLE
                } else {
                    tvRolEnProyecto.visibility = android.view.View.GONE
                }

                // Configurar spinner de permisos
                val permisoOptions = arrayOf("Sin acceso", "Editar")
                val adapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_spinner_item,
                    permisoOptions
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPermiso.adapter = adapter

                // Seleccionar permiso actual
                val currentPosition = when (permission.permiso.name.lowercase()) {
                    "editar" -> 1
                    else -> 0
                }
                spinnerPermiso.setSelection(currentPosition)

                // Guardar cambios
                spinnerPermiso.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        val newPermiso = if (position == 1) "editar" else "ninguno"
                        permissions[permission.userId] = newPermiso
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
            }
        }
    }

    class PermissionDiffCallback : DiffUtil.ItemCallback<ProjectPermission>() {
        override fun areItemsTheSame(oldItem: ProjectPermission, newItem: ProjectPermission): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ProjectPermission, newItem: ProjectPermission): Boolean {
            return oldItem == newItem
        }
    }
}