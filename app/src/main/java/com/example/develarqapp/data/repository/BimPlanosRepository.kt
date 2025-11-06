package com.example.develarqapp.data.repository

import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.BimPlanos
import kotlinx.coroutines.delay

class BimPlanosRepository {

    private val api = ApiConfig.getApiService()

    suspend fun getBimPlans(): Result<List<BimPlanos>> {
        return try {
            // TODO: Implementar llamada real a API
            delay(500)

            // Datos mock
            val mockPlans = listOf(
                BimPlanos(
                    id = 1,
                    title = "04N02-36_GVA_NNN-NNN_AR_M3D_NN_06_Deportivo.ifc",
                    project = "Mocca Master",
                    type = "TXT",
                    url = "http://127.0.0.1:8000...",
                    date = "28/10/2025\n08:57"
                ),
                BimPlanos(
                    id = 2,
                    title = "04N02-36_GVA_NNN-NNN_AR_M3D_NN_06_Deportivo.ifc",
                    project = "Teclado Mecánico",
                    type = "TXT",
                    url = "http://127.0.0.1:8000...",
                    date = "28/10/2025\n08:52"
                )
            )

            Result.success(mockPlans)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBimPlan(planId: Long): Result<Unit> {
        return try {
            // TODO: Implementar descarga real
            delay(200)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBimPlan(planId: Long): Result<Unit> {
        return try {
            // TODO: Implementar eliminación real
            delay(200)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}