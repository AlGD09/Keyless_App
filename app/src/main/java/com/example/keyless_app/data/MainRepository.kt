package com.example.keyless_app.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val cloudClient: CloudClient,
    private val bleManager: BLEManager
) {
    val bleEvents: Flow<BleEvent> = callbackFlow {
        bleManager.onAuthenticated = { rcuId ->
            trySend(BleEvent.Authenticated(rcuId))
        }
        bleManager.onUnlocked = { rcuId ->
            trySend(BleEvent.Unlocked(rcuId))
        }
        bleManager.onChallengeCollectionFinished = { collectedIds ->
            trySend(BleEvent.ChallengeCollectionFinished(collectedIds))
        }

        awaitClose {
            bleManager.onAuthenticated = null
            bleManager.onUnlocked = null
            bleManager.onChallengeCollectionFinished = null
        }
    }

    suspend fun fetchToken(): String? = cloudClient.fetchToken()

    suspend fun fetchAssignedMachines(): List<Machine> = cloudClient.fetchAssignedMachines()

    suspend fun lockMachine(rcuId: String): LockResult = cloudClient.lockMachine(rcuId)

    suspend fun startBleProcess(
        token: String,
        multiMachineMode: Boolean,
        advertiseDurationMs: Long
    ) {
        bleManager.multiMachineMode = multiMachineMode
        bleManager.setToken(token)
        bleManager.stopAdvertising()
        bleManager.startGattServer()
        bleManager.startAdvertisingForDuration(advertiseDurationMs)
    }

    fun stopBleProcess() {
        bleManager.stopAdvertising()
        bleManager.stopGattServer()
    }

    fun processSelectedMachine(rcuId: String) {
        bleManager.processSelectedMachine(rcuId)
    }

    fun startGlobalRssiScan(minCallbackIntervalMs: Long, onResult: (String, Int) -> Unit) {
        bleManager.startGlobalRssiScan(minCallbackIntervalMs, onResult)
    }

    fun stopGlobalRssiScan() {
        bleManager.stopGlobalRssiScan()
    }
}

sealed class BleEvent {
    data class Authenticated(val rcuId: String) : BleEvent()

    data class Unlocked(val rcuId: String) : BleEvent()

    data class ChallengeCollectionFinished(val collectedIds: List<String>) : BleEvent()
}