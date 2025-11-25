package com.example.keyless_app.data

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import javax.inject.Inject
import java.util.UUID

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * BLEManager – verwaltet die Bluetooth-Kommunikation mit der RCU.
 */
class BLEManager @Inject constructor(
    private val context: Context
) {
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var globalScanCallback: ScanCallback? = null
    private var globalScanRunning = false
    // Throttling fuer den globalen RSSI Scan
    private var lastGlobalScanStartMs: Long = 0L
    private val MIN_GLOBAL_SCAN_INTERVAL_MS = 12_000L  // 12 s Abstand
    private var gattServer: BluetoothGattServer? = null
    private var gattServerStarted = false

    private var authToken: String? = null

    // States
    private var responseValue: ByteArray? = null
    private var selectedRcuId: String? = null
    private var collectingChallenges = false



    // Challenge-Handling
    var multiMachineMode: Boolean = false
    private val pendingChallenges = mutableMapOf<String, ByteArray>()
    private val responsesByRcuId = mutableMapOf<String, ByteArray>()

    // Device-Mappings
    private val lastRcuIdByDevice = mutableMapOf<BluetoothDevice, String>()
    private val rcuDeviceById = mutableMapOf<String, BluetoothDevice>()

    // Events
    var onAuthenticated: ((String) -> Unit)? = null

    var onUnlocked: ((String) -> Unit)? = null
    var onChallengeReceived: ((String) -> Unit)? = null
    var onChallengeCollectionFinished: ((List<String>) -> Unit)? = null

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000aaa0-0000-1000-8000-aabbccddeeff")
        val CHAR_RESPONSE: UUID = UUID.fromString("0000aaa1-0000-1000-8001-aabbccddeeff")
        val CHAR_CHALLENGE: UUID = UUID.fromString("0000aaa2-0000-1000-8000-aabbccddeeff")
        private val CCC_DESCRIPTOR_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val CHALLENGE_COLLECTION_MS = 11000L
    }

    // --- BLE SETUP ---

    @SuppressLint("MissingPermission")
    fun startGattServer() {
        if (gattServerStarted) {
            Log.i("BLEManager", "GATT-Server läuft bereits – Überspringe Neustart.")
            return
        }

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServerStarted = true

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val challengeCharacteristic = BluetoothGattCharacteristic(
            CHAR_CHALLENGE,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val responseCharacteristic = BluetoothGattCharacteristic(
            CHAR_RESPONSE,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,  // Permissions for Notifications
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccDescriptor = BluetoothGattDescriptor(
            CCC_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE  // Notifications sowohl zum Schreiben als auch zum Lesen
        )

        // Characteristic Response Descriptor zur Notifications-Aktivierung hinzufügen
        // Client darf die beliebig ein- und ausschalten
        responseCharacteristic.addDescriptor(cccDescriptor)

        service.addCharacteristic(challengeCharacteristic)
        service.addCharacteristic(responseCharacteristic)
        gattServer?.addService(service)

        Log.i("BLEManager", "GATT-Server gestartet mit Service: $SERVICE_UUID")
    }

    @SuppressLint("MissingPermission")
    fun stopGattServer() {
        gattServer?.close()
        gattServer = null
        gattServerStarted = false
        Log.i("BLEManager", "GATT-Server gestoppt.")
    }

    suspend fun startAdvertisingForDuration(durationMs: Long = 30_000L) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e("BLEManager", "BLE Advertising nicht unterstützt.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val manufacturerId = 0xFFFF
        val manufacturerData = deviceId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val data = AdvertiseData.Builder()
            .addManufacturerData(manufacturerId, manufacturerData)
            .setIncludeDeviceName(true)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i("BLEManager", "Advertising gestartet.")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("BLEManager", "Advertising fehlgeschlagen: $errorCode")
            }
        }

        advertiser?.startAdvertising(settings, data, advertiseCallback)
        Log.i("BLEManager", "BLE-Advertising läuft für ${durationMs / 1000}s …")

        delay(durationMs)
        stopAdvertising()
    }



    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        Log.i("BLEManager", "BLE-Advertising gestoppt und Zustand zurückgesetzt.")

        selectedRcuId = null
        pendingChallenges.clear()
        responsesByRcuId.clear()
        rcuDeviceById.clear()
        lastRcuIdByDevice.clear()
        collectingChallenges = false
        responseValue = null
    }

    // --- GATT CALLBACKS ---

    @SuppressLint("MissingPermission")
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i("BLEManager", "Verbindungsstatus: ${device.address}, state=$newState")
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == CCC_DESCRIPTOR_UUID) {
                val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        || value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                Log.i("BLEManager", "Notifications für ${device.address} ${if (enabled) "aktiviert" else "deaktiviert"}")
                if (responseNeeded)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            } else {
                if (responseNeeded)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid != CHAR_CHALLENGE) return

            try {
                // Entsperrt Nachricht behandeln
                val text = value.toString(Charsets.UTF_8)
                if (text == "Entsperrt") {
                    Log.i("BLEManager", "Unlock-Event von RCU empfangen.")
                    val rcuId = selectedRcuId ?: "unknown"
                    onUnlocked?.invoke(rcuId)

                    if (responseNeeded)
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)

                    stopAdvertising()
                    return
                }

                val challenge = value.copyOfRange(0, 16)
                val rcuId = value.copyOfRange(16, value.size).toString(Charsets.UTF_8)
                lastRcuIdByDevice[device] = rcuId
                rcuDeviceById[rcuId] = device

                Log.i("BLEManager", "Challenge von $rcuId empfangen.")

                if (multiMachineMode) {
                    if (selectedRcuId != null && rcuId != selectedRcuId) {
                        Log.i("BLEManager", "Challenge von nicht ausgewählter Maschine ($rcuId) ignoriert.")
                        if (responseNeeded)
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                        return
                    }

                    if (rcuId == selectedRcuId) {
                        val resp = processChallenge(challenge)
                        responsesByRcuId[rcuId] = resp
                        responseValue = resp
                        if (responseNeeded)
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                        sendResponseNotification(rcuId)
                        return
                    }

                    pendingChallenges[rcuId] = challenge
                    onChallengeReceived?.invoke(rcuId)

                    if (!collectingChallenges) {
                        collectingChallenges = true
                        Log.i("BLEManager", "Challenge-Sammelphase gestartet (${CHALLENGE_COLLECTION_MS}ms).")

                        CoroutineScope(Dispatchers.Default).launch {
                            delay(CHALLENGE_COLLECTION_MS)
                            collectingChallenges = false
                            val collectedIds = pendingChallenges.keys.toList()
                            Log.i("BLEManager", "Sammelphase beendet, ${collectedIds.size} Challenges gesammelt.")

                            if (collectedIds.size == 1) {
                                // Nur eine Maschine hat geantwortet -> direkt auswählen
                                val singleId = collectedIds.first()
                                Log.i("BLEManager", "Nur eine Challenge empfangen – Maschine $singleId wird automatisch ausgewählt.")
                                processSelectedMachine(singleId)
                            } else {
                                // Mehrere Challenges -> UI-Auswahl im AblaufScreen
                                onChallengeCollectionFinished?.invoke(collectedIds)
                            }
                            // onChallengeCollectionFinished?.invoke(collectedIds)
                        }
                    }
                } else {
                    selectedRcuId = rcuId
                    val resp = processChallenge(challenge)
                    responsesByRcuId[rcuId] = resp
                    responseValue = resp
                    Log.i("BLEManager", "Response direkt erzeugt.")
                }

                if (responseNeeded)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)

            } catch (e: Exception) {
                Log.e("BLEManager", "Fehler beim Verarbeiten der Challenge: ${e.message}")
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != CHAR_RESPONSE) return

            val rcuId = lastRcuIdByDevice[device]
            val resp = rcuId?.let { responsesByRcuId[it] }

            if (rcuId != null && rcuId == selectedRcuId && resp != null) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, resp)
                onAuthenticated?.invoke(rcuId)
            } else {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                Log.i("BLEManager", "Read verweigert: selected=$selectedRcuId, deviceRcu=$rcuId")
            }
        }
    }

    // --- API METHODS ---

    fun setToken(token: String) {
        authToken = token
    }

    private fun processChallenge(challengeBytes: ByteArray): ByteArray {
        val keyHex = authToken ?: throw IllegalStateException("Kein Token erhalten")
        val keyBytes = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        return mac.doFinal(challengeBytes)
    }

    fun processSelectedMachine(rcuId: String) {
        selectedRcuId = rcuId
        responsesByRcuId.remove(rcuId)
        responseValue = null

        pendingChallenges[rcuId]?.let { challenge ->
            val resp = processChallenge(challenge)
            responsesByRcuId[rcuId] = resp
            responseValue = resp
            sendResponseNotification(rcuId)
        } ?: Log.i("BLEManager", "Warte auf neue Challenge von $rcuId…")
    }

    @SuppressLint("MissingPermission")
    fun sendResponseNotification(rcuId: String) {
        val device = rcuDeviceById[rcuId] ?: run {
            Log.w("BLEManager", "Kein Device zu rcuId=$rcuId gefunden – kein Notify gesendet.")
            return
        }
        val resp = responsesByRcuId[rcuId] ?: return
        val char = gattServer?.getService(SERVICE_UUID)?.getCharacteristic(CHAR_RESPONSE) ?: return

        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                gattServer?.notifyCharacteristicChanged(device, char, false, resp)
                onAuthenticated?.invoke(rcuId)
            } else {
                @Suppress("DEPRECATION")
                char.value = resp
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(device, char, false)
                onAuthenticated?.invoke(rcuId)
            }
            Log.i("BLEManager", "Response an RCU ${device.address} per Notification gesendet.")
        } catch (e: SecurityException) {
            Log.e("BLEManager", "Fehlende Bluetooth-Berechtigung: ${e.message}")
        }
    }

    private fun extractRcuId(result: ScanResult): String? {
        val record = result.scanRecord ?: return null

        // 1) Bevorzugt: Manufacturer ID 0xFFFF
        val manuData = record.manufacturerSpecificData.get(0xFFFF)
        if (manuData != null && manuData.isNotEmpty()) {
            return try {
                val id = manuData.toString(Charsets.UTF_8).trim()
                Log.i("BLEManager", "MSD[0xFFFF] als String: '$id'")
                if (id.isNotEmpty()) return id else null
            } catch (e: Exception) {
                Log.i("BLEManager", "Fehler beim Dekodieren von MSD[0xFFFF]: ${e.message}")
                null
            }
        }

        // 2) Fallback: irgendein Manufacturer-Eintrag als Text versuchen
        val msd = record.manufacturerSpecificData
        for (i in 0 until msd.size()) {
            val key = msd.keyAt(i)
            val bytes = msd.get(key)
            if (bytes != null && bytes.isNotEmpty()) {
                val asText = try {
                    bytes.toString(Charsets.UTF_8).trim()
                } catch (_: Exception) {
                    ""
                }

                if (asText.isNotEmpty()) {
                    Log.i(
                        "BLEManager",
                        "Fallback MSD key=0x${key.toString(16)} als Text: '$asText'"
                    )
                    return asText
                } else {
                    Log.i(
                        "BLEManager",
                        "Fallback MSD key=0x${key.toString(16)} bytes=${bytes.joinToString(" ") { "%02X".format(it) }}"
                    )
                }
            }
        }

        // 3) Fallback: Device Name (sehr häufig so gesetzt wie deine RCU-ID)
        Log.i(
            "BLEManager",
            "Keine RCU-ID aus Advertisement extrahiert fuer ${result.device.address}"
        )
        return null
    }

    @SuppressLint("MissingPermission")
    fun startGlobalRssiScan(minCallbackIntervalMs: Long = 2_000L, onResult: (String, Int) -> Unit) {
        // Wenn schon ein Scan laeuft, nicht noch einmal starten
        if (globalScanRunning) {
            Log.i("BLEManager", "Globaler RSSI Scan laeuft bereits.")
            return
        }

        val now = System.currentTimeMillis()
        val diff = now - lastGlobalScanStartMs

        val lastCallbackByRcuId = mutableMapOf<String, Long>()

        // Harte Ratebegrenzung: nicht oefter als alle 35 s einen neuen Start erlauben
        if (diff in 1 until MIN_GLOBAL_SCAN_INTERVAL_MS) {
            Log.i(
                "BLEManager",
                "Scan Anforderung verworfen, zu haeufig (letzter Start vor ${diff} ms)."
            )
            return
        }

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: run {
            Log.i("BLEManager", "Kein Bluetooth Adapter gefunden.")
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.i("BLEManager", "Kein BluetoothLeScanner verfuegbar.")
            return
        }

        lastGlobalScanStartMs = now

        globalScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val addr = result.device.address
                val name = result.device.name ?: result.scanRecord?.deviceName ?: "N/A"
                val rssi = result.rssi
                //Log.i("BLEManager", "SCAN → addr=$addr  name=$name  rssi=$rssi")

                if (name.startsWith("Maschine_")) {
                    // 2. Extraer el RCU ID después del guión bajo
                    val rcuId = name.removePrefix("Maschine_")

                    // Log.i("BLEManager", "Maschine gefunden: rcuId=$rcuId  rssi=$rssi")

                    // Pro Maschine RSSI Check jede 2s
                    val nowMs = System.currentTimeMillis()
                    val lastCallbackMs = lastCallbackByRcuId[rcuId] ?: 0L
                    val intervalMs = nowMs - lastCallbackMs

                    if (intervalMs >= minCallbackIntervalMs) {
                        Log.i(
                            "BLEManager",
                            "Maschine gefunden: rcuId=$rcuId  rssi=$rssi (alle ${minCallbackIntervalMs}ms)"
                        )
                        lastCallbackByRcuId[rcuId] = nowMs
                        onResult(rcuId, rssi)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                // Nur fuer Logging, globalScanRunning zuruecksetzen
                Log.i("BLEManager", "Globaler Scan fehlgeschlagen, Code=$errorCode")
                globalScanRunning = false
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()


        try {
            scanner.startScan(null, settings, globalScanCallback!!)
            globalScanRunning = true
            Log.i("BLEManager", "Globaler RSSI Scan gestartet.")
        } catch (e: Exception) {
            Log.e("BLEManager", "Fehler beim Starten des Global Scans: ${e.message}")
            globalScanCallback = null
            globalScanRunning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopGlobalRssiScan() {
        if (!globalScanRunning) {
            Log.i("BLEManager", "Globaler RSSI Scan war nicht aktiv, nichts zu stoppen.")
            return
        }

        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return
            val scanner = adapter.bluetoothLeScanner ?: return

            globalScanCallback?.let { cb ->
                scanner.stopScan(cb)
                Log.i("BLEManager", "Globaler RSSI Scan gestoppt.")
            }
        } catch (e: Exception) {
            Log.e("BLEManager", "Fehler beim Stoppen des Global Scans: ${e.message}")
        } finally {
            globalScanCallback = null
            globalScanRunning = false

            // Kleine Pause, hilft bei Xiaomi/MIUI
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
            }
        }
    }

}
