package com.example.keyless_app.data
import kotlinx.serialization.Serializable

@Serializable
data class UnlockedMachine(
    val rcuId: String,
    val name: String,
    val location: String
)