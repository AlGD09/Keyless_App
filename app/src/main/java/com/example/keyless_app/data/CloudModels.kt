package com.example.keyless_app.data

data class TokenRequest(
    val userName: String,
    val deviceId: String,
    val secretHash: String
)

data class TokenResponse(
    val auth_token: String?
)