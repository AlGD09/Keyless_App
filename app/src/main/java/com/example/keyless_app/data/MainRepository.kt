package com.example.keyless_app.data

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MainRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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

    suspend fun fetchToken(): String? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", "") ?: ""
        val secretHash = prefs.getString("secretHash", "") ?: ""
        return cloudClient.fetchToken(userName, secretHash)
    }

    fun isUserRegistered(): Boolean {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", null)
        val secretHash = prefs.getString("secretHash", null)
        return !userName.isNullOrBlank() && !secretHash.isNullOrBlank()
    }

    fun saveCredentials(userName: String, secretHash: String) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("userName", userName)
            .putString("secretHash", secretHash)
            .apply()
    }

    fun clearCredentials() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    suspend fun fetchAssignedMachines(): List<Machine> = cloudClient.fetchAssignedMachines()

    suspend fun lockMachine(rcuId: String): LockResult = cloudClient.lockMachine(rcuId)

    fun unlockedMachinesFlow(): Flow<List<UnlockedMachine>> {
        return context.unlockedMachinesDataStore.data.map { it.machines }
    }

    suspend fun saveUnlockedMachines(machines: List<UnlockedMachine>) {
        context.unlockedMachinesDataStore.updateData { state ->
            state.copy(machines = machines)
        }
    }

    fun getUserName(): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("userName", "") ?: "Unbekannt"
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

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