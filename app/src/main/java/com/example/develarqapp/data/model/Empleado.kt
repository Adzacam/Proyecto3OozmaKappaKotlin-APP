package com.example.develarqapp.data.model

class Empleado {

    data class Empleado(
        val id: String = "",
        val nombre: String = "",
        val apellido: String = "",
        val email: String = "",
        val password: String = "",
        val rol: RolEmpleado = RolEmpleado.EMPLEADO,
        val activo: Boolean = true,
        val fechaCreacion: Long = System.currentTimeMillis()
    )

    enum class RolEmpleado {
        ADMINISTRADOR,
        EMPLEADO,
        COORDINADOR
    }

    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class LoginResponse(
        val success: Boolean,
        val message: String,
        val empleado: Empleado? = null,
        val token: String? = null
    )

    data class EmpleadoUI(
        val id: String,
        val nombreCompleto: String,
        val email: String,
        val rol: String,
        val activo: Boolean
    ) {
        companion object {
            fun fromEmpleado(empleado: Empleado): EmpleadoUI {
                return EmpleadoUI(
                    id = empleado.id,
                    nombreCompleto = "${empleado.nombre} ${empleado.apellido}",
                    email = empleado.email,
                    rol = empleado.rol.name,
                    activo = empleado.activo
                )
            }
        }
    }
}