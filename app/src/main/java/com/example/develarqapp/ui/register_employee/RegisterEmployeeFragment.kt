package com.example.develarqapp.ui.register_employee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class RegisterEmployeeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Por ahora retornamos una vista temporal
        val textView = TextView(requireContext())
        textView.text = "Registrar Empleado\n(Fragment por implementar)"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}
