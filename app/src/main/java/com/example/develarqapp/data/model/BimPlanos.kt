package com.example.develarqapp.data.model

data class BimPlanos(
    val id: Long,
    val title: String,
    val project: String,
    val type: String, // TXT, PDF, IFC, etc.
    val url: String,
    val date: String,
    val description: String? = null
)
