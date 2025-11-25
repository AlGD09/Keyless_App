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
import com.example.keyless_app.data.LockResult
import android.provider.Settings
import com.example.keyless_app.data.Machine
import com.example.keyless_app.data.UnlockedMachine
import com.example.keyless_app.data.unlockedMachinesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlin.collections.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.collections.firstOrNull

@HiltViewModel
class AblaufViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudClient: CloudClient,
    private val bleManager: BLEManager
) : ViewModel() {
    private var currentJob: Job? = null
    private var rssiMonitorJob: Job? = null  // Job zur RSSI Überwachung entsperrter Maschinen
    private var unlockSignal = CompletableDeferred<Unit>()
    private val lastRssiTimestamps = mutableMapOf<String, Long>()
    private val RSSI_TIMEOUT_MS = 20_000L
    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status = _status.asStateFlow()

    private val _machines = MutableStateFlow<List<Machine>>(emptyList())
    val machines: StateFlow<List<Machine>> = _machines

    private val _showUserInfoDialog = MutableStateFlow(false)
    val showUserInfoDialog = _showUserInfoDialog.asStateFlow()

    // Maschinenauswahl
    private val _pendingMachines = MutableStateFlow<List<String>>(emptyList())
    val pendingMachines = _pendingMachines.asStateFlow()

    // Authentifizierte Maschine
    private val _authenticatedMachine = MutableStateFlow<String?>(null)
    val authenticatedMachine = _authenticatedMachine.asStateFlow()

    //Bei Warnungsmeldung Maschinenname korrekt anzeigen
    private val _lockedMachine = MutableStateFlow<String?>(null)
    val lockedMachine = _lockedMachine.asStateFlow()

    // Entriegelte Maschinen
    private val _unlockedMachines = MutableStateFlow<List<UnlockedMachine>>(emptyList())
    val unlockedMachines = _unlockedMachines.asStateFlow()



    init {
        bleManager.onAuthenticated = { rcuId ->
            viewModelScope.launch {
                _authenticatedMachine.value = rcuId

                _status.value = Status.Authentifiziert
            }
        }

        bleManager.onUnlocked = { rcuId ->
            viewModelScope.launch {

                // Maschine ein einziges Mal suchen
                val machine = _machines.value.firstOrNull { it.rcuId == rcuId }

                val name = machine?.name ?: "Unbekannte Maschine"
                val location = machine?.location ?: "Unbekannter Standort"

                // RCU in Liste der entriegelten Maschinen eintragen
                val current = _unlockedMachines.value.toMutableList()
                if (current.none { it.rcuId == rcuId }) {
                    current.add(
                        UnlockedMachine(
                            rcuId = rcuId,
                            name = name,
                            location = location
                        )
                    )
                    _unlockedMachines.value = current
                    context.unlockedMachinesDataStore.updateData { state ->
                        state.copy(machines = current)
                    }
                    Log.i("AblaufViewModel", "Entsperrte Maschinen registriert")
                }
                _status.value = Status.Entsperrt
                kotlinx.coroutines.delay(4000)

                if (!unlockSignal.isCompleted) {
                    unlockSignal.complete(Unit)
                }

                startRssiMonitor()


            }

        }

        bleManager.onChallengeCollectionFinished = { collectedIds ->
            viewModelScope.launch {
                if (collectedIds.isNotEmpty()) {
                    Log.i("AblaufViewModel", "Challenges empfangen von ${collectedIds.size} Maschinen: $collectedIds")
                    _pendingMachines.value = collectedIds
                    _status.value = Status.Auswahl
                } else {
                    Log.i("AblaufViewModel", "Keine Challenges empfangen.")
                }
            }
        }

        // Beim Start alle noch offene Maschinen anzeigen
        viewModelScope.launch {
            context.unlockedMachinesDataStore.data.collect { state ->
                _unlockedMachines.value = state.machines

                // RSSI-Check auch nach App Kill wiederstarten
                /*if (_unlockedMachines.value.isNotEmpty()){
                    startRssiMonitor()
                }*/
            }
        }

    }

    fun startProcess() {
        // Falls noch ein alter Prozess läuft, abbrechen:
        currentJob?.cancel()
        unlockSignal = CompletableDeferred()

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
                if (assignedMachines.size > 1) {
                    bleManager.multiMachineMode = true
                } else {
                    bleManager.multiMachineMode = false
                }
                _status.value = Status.CloudSuccess
                bleManager.setToken(token)
                _status.value = Status.BLEStarting
                // bleManager.stopGattServer()      // <- alter GATT-Server beenden
                bleManager.stopAdvertising()     // <- altes Advertising stoppen
                bleManager.startGattServer()
                _status.value = Status.BLEServer
                _status.value = Status.BLEAdvertise
                // bleManager.startAdvertisingForDuration(45_000)
                launch {
                    bleManager.startAdvertisingForDuration(45_000)
                }
                // Adervtising Dauer unterbrechen falls Entsperrt kommt
                withTimeoutOrNull(45_000) {
                    unlockSignal.await()
                }
                _status.value = Status.BLEStopped
                _authenticatedMachine.value = null // Variable zurücksetzen wenn Prozess abgeschlossen ist
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

    fun handleMachineSelection(rcuId: String) {
        viewModelScope.launch {
            try {
                Log.i("AblaufViewModel", "Benutzer hat $rcuId gewählt.")
                _status.value = Status.BLEProcessing

                // BLEManager übergeben, welche Maschine verarbeitet werden soll
                bleManager.processSelectedMachine(rcuId)

                // Jetzt macht BLEManager alles Weitere selbst.
                // Das ViewModel wartet darauf, dass BLEManager.onAuthenticated aufgerufen wird.

            } catch (e: Exception) {
                Log.e("AblaufViewModel", "Fehler bei handleMachineSelection: ${e.message}")
                _status.value = Status.Error
            }
        }
    }

    fun lockMachine(rcuId: String) {
        _lockedMachine.value = _unlockedMachines.value
            .firstOrNull { it.rcuId == rcuId }
            ?.name ?: rcuId

        viewModelScope.launch {
            _status.value = Status.Lock

            when (cloudClient.lockMachine(rcuId)) {
                LockResult.ACCEPTED -> {
                    _status.value = Status.Locked
                    // Aus der Liste entfernen
                    val list = _unlockedMachines.value.toMutableList()
                    list.removeAll { it.rcuId == rcuId }
                    lastRssiTimestamps.remove(rcuId)
                    _unlockedMachines.value = list
                    context.unlockedMachinesDataStore.updateData { state ->
                        state.copy(machines = list)
                    }
                    if (_unlockedMachines.value.isEmpty()) {
                        rssiMonitorJob?.cancel()
                        rssiMonitorJob = null
                        bleManager.stopGlobalRssiScan()
                    }
                    kotlinx.coroutines.delay(2000)
                    _status.value = Status.Idle
                }

                LockResult.TIMEOUT -> {
                    _status.value = Status.LockTimeout
                    val list = _unlockedMachines.value.toMutableList()
                    list.removeAll { it.rcuId == rcuId }
                    lastRssiTimestamps.remove(rcuId)
                    _unlockedMachines.value = list
                    context.unlockedMachinesDataStore.updateData { state ->
                        state.copy(machines = list)
                    }
                    if (_unlockedMachines.value.isEmpty()) {
                        rssiMonitorJob?.cancel()
                        rssiMonitorJob = null
                        bleManager.stopGlobalRssiScan()
                    }
                    kotlinx.coroutines.delay(5000)
                    _status.value = Status.Idle
                }

                LockResult.DEPRECATED -> {
                    _status.value = Status.LockDeprecated
                    val list = _unlockedMachines.value.toMutableList()
                    list.removeAll { it.rcuId == rcuId }
                    lastRssiTimestamps.remove(rcuId)
                    _unlockedMachines.value = list
                    context.unlockedMachinesDataStore.updateData { state ->
                        state.copy(machines = list)
                    }
                    if (_unlockedMachines.value.isEmpty()) {
                        rssiMonitorJob?.cancel()
                        rssiMonitorJob = null
                        bleManager.stopGlobalRssiScan()
                    }
                    kotlinx.coroutines.delay(5000)
                    _status.value = Status.Idle
                }

                LockResult.ERROR -> {
                    _status.value = Status.LockError
                    kotlinx.coroutines.delay(5000)
                    _status.value = Status.Idle
                }
            }
        }
    }

    private fun handleRssiUpdate(values: Map<String, Int>) {
        values.forEach { (id, rssi) ->
            Log.i("RSSI", "RCU $id hat RSSI = $rssi")
            lastRssiTimestamps[id] = System.currentTimeMillis()
            if (rssi < -65) {
                lockMachine(id)
                Log.i("RSSI", "RCU $id wird verriegelt")
            }
        }
    }

    fun startRssiMonitor() {
        rssiMonitorJob?.cancel()

        rssiMonitorJob = viewModelScope.launch {
            val unlocked = _unlockedMachines.value.map { it.rcuId }

            if (unlocked.isEmpty()) {
                Log.i("AblaufViewModel", "Kein RSSI-Monitoring nötig.")
                return@launch
            }

            // Initialisierung pro Maschine
            val startTimestamp = System.currentTimeMillis()
            unlocked.forEach { rcuId ->
                lastRssiTimestamps[rcuId] = startTimestamp
            }

            bleManager.startGlobalRssiScan(minCallbackIntervalMs = 2_000L) { id, rssi ->
                val currentlyUnlocked = _unlockedMachines.value.map { it.rcuId }  // Damit die Liste der aktuellen unlocked Maschinen immer aktuell bleibt

                if (currentlyUnlocked.contains(id)) {
                    lastRssiTimestamps[id] = System.currentTimeMillis()
                    handleRssiUpdate(mapOf(id to rssi))
                }
            }

            while (isActive) {
                delay(1_000L)
                val now = System.currentTimeMillis()
                val currentlyUnlocked = _unlockedMachines.value.map { it.rcuId }
                val timedOut = currentlyUnlocked.filter { id ->
                    val lastSeen = lastRssiTimestamps[id] ?: now
                    now - lastSeen >= RSSI_TIMEOUT_MS
                }

                timedOut.forEach { id ->
                    Log.i(
                        "RSSI",
                        "RCU $id seit ${RSSI_TIMEOUT_MS / 1000}s nicht gesehen – Verriegelung wird ausgelöst"
                    )
                    lastRssiTimestamps[id] = now
                    lockMachine(id)
                }
            }
        }
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
    object Auswahl : Status("Maschine auswählen")
    object BLEProcessing: Status("Ausgewählte Maschine wird entsperrt...")
    object BLEStopped : Status("BLE gestoppt.")
    object Authentifiziert : Status("Authentifizierung erfolgreich")
    object Entsperrt : Status("Maschine erfolgreich entriegelt")
    object Lock : Status("Maschine wird verriegelt")
    object Locked : Status("Maschine erfolgreich verriegelt")
    object ErrorToken : Status ("Gerät oder User nicht authentifiziert")
    object Error : Status ("Cloud- oder BLE Fehler")
    object LockError : Status ("Fehler beim Verriegeln der Maschine")
    object LockTimeout : Status ("Fehler: Maschine ist nicht erreichbar")
    object LockDeprecated : Status ("Fehler: Gerät wird bereits von einem anderen verwaltet")
}



