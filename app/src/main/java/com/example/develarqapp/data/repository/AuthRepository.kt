package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.UpdateUserRequest
import com.example.develarqapp.data.model.UpdatePasswordRequest

class AuthRepository {

    private val api = ApiConfig.getApiService()

    suspend fun updateUserProfile(
        id: Long,
        name: String,
        apellido: String,
        email: String,
        telefono: String?,
        rol: String,
        password: String?,
        token: String
    ): Result<Unit> {
        return try {
            val request = UpdateUserRequest(id, name, apellido, email, telefono, rol, password)
            val response = api.updateUser(request, "Bearer $token")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al actualizar perfil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String,
        token: String
    ): Result<Unit> {
        return try {
            val request = UpdatePasswordRequest(userId, currentPassword, newPassword)
            val response = api.updatePassword(request, "Bearer $token")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Contraseña incorrecta o error de servidor"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
