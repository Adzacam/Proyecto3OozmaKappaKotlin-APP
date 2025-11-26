package com.example.develarqapp.ui.users

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.develarqapp.R
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.DialogEditUserBinding
import com.example.develarqapp.utils.PasswordValidator
import com.example.develarqapp.utils.SessionManager

class EditUserDialogFragment : DialogFragment() {

    private var _binding: DialogEditUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsersViewModel by activityViewModels()
    private var user: User? = null

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_APELLIDO = "user_apellido"
        private const val ARG_USER_EMAIL = "user_email"
        private const val ARG_USER_PHONE = "user_phone"
        private const val ARG_USER_ROL = "user_rol"

        fun newInstance(user: User) = EditUserDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_USER_ID, user.id)
                putString(ARG_USER_NAME, user.name)
                putString(ARG_USER_APELLIDO, user.apellido)
                putString(ARG_USER_EMAIL, user.email)
                putString(ARG_USER_PHONE, user.telefono)
                putString(ARG_USER_ROL, user.rol)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupUI()
        setupRoleDropdown()
        setupPasswordValidation()
        setupObservers()
    }

    private fun loadUserData() {
        arguments?.let { args ->
            user = User(
                id = args.getLong(ARG_USER_ID),
                name = args.getString(ARG_USER_NAME) ?: "",
                apellido = args.getString(ARG_USER_APELLIDO) ?: "",
                email = args.getString(ARG_USER_EMAIL) ?: "",
                telefono = args.getString(ARG_USER_PHONE),
                rol = args.getString(ARG_USER_ROL) ?: "",
                estado = "activo"//,
                //createdAt = null
            )

            binding.etName.setText(user?.name)
            binding.etLastName.setText(user?.apellido)
            binding.etEmail.setText(user?.email)
            binding.etPhone.setText(user?.telefono)
            binding.actvRole.setText(user?.rol, false)
        }
    }

    private fun setupUI() {
        // Switch para mostrar/ocultar cambio de contraseña
        binding.switchChangePassword.setOnCheckedChangeListener { _, isChecked ->
            binding.llPasswordSection.isVisible = isChecked
        }

        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Botón actualizar
        binding.btnUpdate.setOnClickListener {
            updateUser()
        }
    }

    private fun setupRoleDropdown() {
        val roles = listOf("admin", "ingeniero", "arquitecto", "cliente")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            roles
        )
        binding.actvRole.setAdapter(adapter)
    }

    private fun setupPasswordValidation() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    val validation = PasswordValidator.validatePassword(password)
                    updatePasswordIndicators(validation)
                }
            }
        })
    }

    private fun updatePasswordIndicators(validation: PasswordValidator.PasswordValidationResult) {
        val validColor = Color.parseColor("#4ADE80")
        val invalidColor = Color.parseColor("#F87171")

        validation.rules.forEachIndexed { index, rule ->
            val textView = when (index) {
                0 -> binding.tvPasswordLen
                1 -> binding.tvPasswordUpper
                2 -> binding.tvPasswordLower
                3 -> binding.tvPasswordNum
                4 -> binding.tvPasswordSym
                5 -> binding.tvPasswordSpc
                else -> null
            }
            textView?.setTextColor(if (rule.isValid) validColor else invalidColor)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.llButtons.isVisible = !isLoading
        }
    }

    private fun updateUser() {
        user?.let {

            val name = binding.etName.text.toString()
            val apellido = binding.etLastName.text.toString()
            val email = binding.etEmail.text.toString()
            val phone = binding.etPhone.text.toString()
            val rol = binding.actvRole.text.toString()

            val password = if (binding.switchChangePassword.isChecked) {
                binding.etPassword.text.toString()
            } else {
                null
            }
            val sessionManager = SessionManager(requireContext())
            val token = sessionManager.getToken()

            if (token == null) {
                // Aquí puedes mostrar un Toast o navegar al login
                // Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
                dismiss() // Cierra el diálogo para evitar más acciones
                return@let // Sale de la función 'let'
            }
            viewModel.updateUser(
                id = it.id,
                name = name,
                apellido = apellido,
                email = email,
                phone = phone.ifEmpty { null },
                rol = rol,
                password = password,
                token = token
            )

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
