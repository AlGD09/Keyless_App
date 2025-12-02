package com.example.keyless_app.data

data class TokenRequest(
    val userName: String,
    val deviceId: String,
    val secretHash: String
)

data class TokenResponse(
    val auth_token: String?
)

data class LockResponse(
    val rcuId: String,
    val status: String
)

enum class LockResult {
    ACCEPTED,
    DEPRECATED,
    TIMEOUT,
    ERROR
}

data class LockRequest(
    val rcuId: String,
    val deviceName: String,
    val deviceId: String
)

data class RcuResponse(
    val id: Int,
    val rcuId: String,
    val name: String,
    val location: String,
    val registeredAt: String,
    val status: String,
    val assignedSmartphone: AssignedSmartphone?
)

data class AssignedSmartphone(
    val id: Int,
    val deviceId: String,
    val userName: String,
    val secretHash: String,
    val bleId: String?,
    val status: String,
    val lastSeen: String?
)

data class Machine(
    val rcuId: String,
    val name: String,
    val location: String,
    val status: String
)