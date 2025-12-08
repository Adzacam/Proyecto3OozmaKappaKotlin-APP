package com.example.develarqapp.ui.users

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.develarqapp.R
import com.example.develarqapp.data.model.User
import com.example.develarqapp.databinding.DialogDeleteUserReasonBinding

class DeleteUserDialogFragment : DialogFragment() {

    private var _binding: DialogDeleteUserReasonBinding? = null
    private val binding get() = _binding!!

    private var user: User? = null
    private var onConfirmListener: ((String) -> Unit)? = null

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_APELLIDO = "user_apellido"
        private const val MIN_CHARACTERS = 30

        fun newInstance(user: User, onConfirm: (String) -> Unit): DeleteUserDialogFragment {
            return DeleteUserDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, user.id)
                    putString(ARG_USER_NAME, user.name ?: "")
                    putString(ARG_USER_APELLIDO, user.apellido ?: "")
                }
                this.onConfirmListener = onConfirm
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
        _binding = DialogDeleteUserReasonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupUI()
        setupListeners()
    }

    private fun loadUserData() {
        arguments?.let { args ->
            val name = args.getString(ARG_USER_NAME, "")
            val apellido = args.getString(ARG_USER_APELLIDO, "")
            val fullName = "$name $apellido".trim()

            binding.tvSubtitle.text = "¿Está seguro de eliminar a $fullName?"
        }
    }

    private fun setupUI() {
        // Inicialmente el botón está deshabilitado
        binding.btnConfirm.isEnabled = false
        binding.tvValidation.visibility = View.GONE
    }

    private fun setupListeners() {
        // Monitorear cambios en el texto
        binding.etMotivo.addTextChangedListener { text ->
            val length = text?.length ?: 0
            updateValidation(length)
        }

        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Botón confirmar
        binding.btnConfirm.setOnClickListener {
            val motivo = binding.etMotivo.text.toString().trim()
            if (motivo.length >= MIN_CHARACTERS) {
                onConfirmListener?.invoke(motivo)
                dismiss()
            }
        }
    }

    private fun updateValidation(length: Int) {
        if (length < MIN_CHARACTERS) {
            binding.tvValidation.visibility = View.VISIBLE
            binding.tvValidation.text = "$length/$MIN_CHARACTERS caracteres"
            binding.tvValidation.setTextColor(
                resources.getColor(R.color.error, null)
            )
            binding.btnConfirm.isEnabled = false
        } else {
            binding.tvValidation.visibility = View.VISIBLE
            binding.tvValidation.text = "$length caracteres ✓"
            binding.tvValidation.setTextColor(
                resources.getColor(R.color.success, null)
            )
            binding.btnConfirm.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}