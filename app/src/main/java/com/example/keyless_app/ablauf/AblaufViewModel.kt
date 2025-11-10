package com.example.keyless_app.ablauf

import android.content.Context
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyless_app.data.BLEManager
import com.example.keyless_app.data.CloudClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import android.provider.Settings
import com.example.keyless_app.data.Machine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job

@HiltViewModel
class AblaufViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudClient: CloudClient,
    private val bleManager: BLEManager
) : ViewModel() {
    private var currentJob: Job? = null
    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status = _status.asStateFlow()

    private val _machines = MutableStateFlow<List<Machine>>(emptyList())
    val machines: StateFlow<List<Machine>> = _machines

    private val _showUserInfoDialog = MutableStateFlow(false)
    val showUserInfoDialog = _showUserInfoDialog.asStateFlow()

    init {
        bleManager.onAuthenticated = {
            viewModelScope.launch {
                _status.value = Status.Authentifiziert

                kotlinx.coroutines.delay(5000)

            }
        }
    }

    fun startProcess() {
        // Falls noch ein alter Prozess läuft, abbrechen:
        currentJob?.cancel()

        //Coroutine starten
        currentJob = viewModelScope.launch {
            _status.value = Status.CloudConnecting
            try {
                val token = cloudClient.fetchToken() ?: throw Exception("Cloud-Fehler")
                if (token.isEmpty()){
                    _status.value = Status.ErrorToken
                }
                _status.value = Status.LoadingMachines
                val assignedMachines = cloudClient.fetchAssignedMachines()
                _machines.value = assignedMachines
                _status.value = Status.CloudSuccess
                bleManager.setToken(token)
                _status.value = Status.BLEStarting
                bleManager.stopGattServer()      // <- alter GATT-Server beenden
                bleManager.stopAdvertising()     // <- altes Advertising stoppen
                bleManager.startGattServer()
                _status.value = Status.BLEServer
                _status.value = Status.BLEAdvertise
                bleManager.startAdvertisingForDuration(45_000)
                _status.value = Status.BLEStopped
                _machines.value = emptyList()
                bleManager.stopGattServer()
                _status.value = Status.Idle
            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                    _status.value = Status.ErrorToken
                    Log.e("AblaufViewModel", "Authentifizierungsfehler: ${e.message}")
                } else {
                    _status.value = Status.Error
                    Log.e("AblaufViewModel", "Allgemeiner Fehler: ${e.message}")
                }
            }
        }
    }

    fun toggleUserInfoDialog() {
        _showUserInfoDialog.value = !_showUserInfoDialog.value
    }

    fun getUserName(): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("userName", "") ?: "Unbekannt"
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

}

sealed class Status(val label: String) {
    object Idle : Status("Bereit")
    object CloudConnecting : Status("Verbindungsversuch mit der Cloud")
    object LoadingMachines : Status("Maschinen werden abgerufen")
    object CloudSuccess : Status("Cloud-Verbindung erfolgreich")
    object BLEStarting : Status("BLE: Läuft...")
    object BLEServer : Status("GATT Server gestartet")
    object BLEAdvertise : Status("Advertising gestartet für 45s")
    object BLEStopped : Status("BLE gestoppt.")
    object Authentifiziert : Status("Authentifizierung erfolgreich")
    object ErrorToken : Status ("Gerät oder User nicht authentifiziert")
    object Error : Status ("Cloud- oder BLE Fehler")
}



