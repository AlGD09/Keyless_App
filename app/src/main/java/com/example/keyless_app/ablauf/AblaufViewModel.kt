package com.example.keyless_app.ablauf

import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyless_app.data.BLEManager
import com.example.keyless_app.data.CloudClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class AblaufViewModel @Inject constructor(
    private val cloudClient: CloudClient,
    private val bleManager: BLEManager
) : ViewModel() {

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status = _status.asStateFlow()

    fun startProcess() {
        viewModelScope.launch {
            _status.value = Status.CloudConnecting
            try {
                val token = cloudClient.fetchToken() ?: throw Exception("Cloud-Fehler")
                if (token.isEmpty()){
                    _status.value = Status.ErrorToken
                }
                _status.value = Status.CloudSuccess
                bleManager.setToken(token)
                _status.value = Status.BLEStarting
                bleManager.startGattServer()
                _status.value = Status.BLEServer
                _status.value = Status.BLEAdvertise
                bleManager.startAdvertisingForDuration(30_000)


                _status.value = Status.BLEStopped
                bleManager.stopGattServer()
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

    fun retry() {
        _status.value = Status.Idle
    }
}

sealed class Status(val label: String) {
    object Idle : Status("Bereit")
    object CloudConnecting : Status("Cloud: Verbindung...")
    object CloudSuccess : Status("Cloud: Erfolgreich")
    object BLEStarting : Status("BLE: Läuft...")
    object BLEServer : Status("GATT Server gestartet")
    object BLEAdvertise : Status("Advertising gestartet für 30s")
    object BLEStopped : Status("BLE gestoppt.")
    object AuthSuccess : Status("Authentifiziert")
    object ErrorToken : Status ("Gerät nicht authentifiziert")
    object Error : Status ("Cloud- oder BLE Fehler")
}



