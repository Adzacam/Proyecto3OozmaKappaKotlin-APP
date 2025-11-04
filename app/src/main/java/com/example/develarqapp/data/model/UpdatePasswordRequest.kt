package com.example.develarqapp.data.model

import com.google.gson.annotations.SerializedName

data class UpdatePasswordRequest(
    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("current_password")
    val currentPassword: String,

    @SerializedName("new_password")
    val newPassword: String
)
