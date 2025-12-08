package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

data class AuditoriaLog(
    @SerializedName("id")
    val id: Long,

    @SerializedName("usuario")
    val usuario: String,

    @SerializedName("accion")
    val accion: String,

    @SerializedName("registro")
    val registro: String,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("ip_address")
    val ip_address: String? = null,

    @SerializedName("descripcion_detallada")
    val descripcionDetallada: String? = null,


    @SerializedName("tabla_afectada")
    val tablaAfectada: String? = null
)