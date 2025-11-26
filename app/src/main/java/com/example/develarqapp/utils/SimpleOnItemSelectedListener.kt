package com.example.develarqapp.utils

import android.view.View
import android.widget.AdapterView

/**
 * Helper para simplificar el listener de Spinner
 * Evita triggers en la selección inicial
 */
class SimpleOnItemSelectedListener(
    private val onItemSelected: (position: Int) -> Unit
) : AdapterView.OnItemSelectedListener {

    private var isInitialSelection = true

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (isInitialSelection) {
            isInitialSelection = false
            return
        }
        onItemSelected(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // No hacer nada
    }

    /**
     * Resetea el flag para evitar triggers en setSelection programático
     */
    fun reset() {
        isInitialSelection = true
    }
}