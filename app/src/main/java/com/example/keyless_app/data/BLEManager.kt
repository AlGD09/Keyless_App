package com.example.keyless_app.data

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID
import android.provider.Settings
/**
 * BLEManager – verwaltet die Bluetooth-Kommunikation mit der RCU.
 * Zunächst als Dummy-Implementierung mit simuliertem Ablauf.
 */
class BLEManager @Inject constructor(
    private val context: Context
) {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var gattServer: BluetoothGattServer? = null
    private var responseValue: ByteArray? = null

    private var authToken : String? = null

    var onAuthenticated: (() -> Unit)? = null

    //Multimapping Maschinenauswahl
    var multiMachineMode: Boolean = false
    private val pendingChallenges = mutableMapOf<String, ByteArray>()
    var onChallengeReceived: ((String) -> Unit)? = null
    var onChallengeCollectionFinished: ((List<String>) -> Unit)? = null
    private var collectingChallenges = false

    //Nach Maschinen Auswahl nur Challenges von dieser Maschine weiter behandeln
    private var selectedRcuId: String? = null


    /**
     * Startet den BLE-Authentifizierungsprozess.
     * @param token Token, das von der Cloud erhalten wurde.
     * @return true, wenn Authentifizierung erfolgreich, false sonst.
     */

    suspend fun startAdvertisingForDuration(durationMs: Long = 30_000L) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            Log.e("BLEManager", "Bluetooth ist deaktiviert oder nicht verfügbar.")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e("BLEManager", "BLE Advertising wird auf diesem Gerät nicht unterstützt.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)  // Später "true" für die RCU wichtig
            .build()

        // Manufacturer Data – Company ID 0xFFFF
        val manufacturerId = 0xFFFF
        fun hexStringToByteArray(hex: String): ByteArray {
            require(hex.length % 2 == 0) { "Ungültige Hex-Länge" }
            return ByteArray(hex.length / 2) { i ->
                ((hex.substring(i * 2, i * 2 + 2).toInt(16)) and 0xFF).toByte()
            }
        }

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceIdBytes = hexStringToByteArray(deviceId)
        // Device ID bd45e75870af93c2
        /*val deviceIdBytes = byteArrayOf(
            0xBD.toByte(), 0x45.toByte(), 0xE7.toByte(), 0x58.toByte(),
            0x70.toByte(), 0xAF.toByte(), 0x93.toByte(), 0xC2.toByte()
        )*/

        val data = AdvertiseData.Builder()
            .addManufacturerData(manufacturerId, deviceIdBytes)
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
        Log.i("BLEManager", "BLE-Advertising läuft für ${durationMs / 1000} Sekunden …")

        // Zeit abwarten
        delay(durationMs)

        stopAdvertising()
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        Log.i("BLEManager", "BLE-Advertising gestoppt.")
        // Zustand zurücksetzen, um neuen Authentifizierungszyklus vorzubereiten
        selectedRcuId = null
        pendingChallenges.clear()
        collectingChallenges = false
        Log.i("BLEManager", "Zustand zurückgesetzt (selectedRcuId=null, pendingChallenges gelöscht).")
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000aaa0-0000-1000-8000-aabbccddeeff")
        val CHAR_RESPONSE: UUID = UUID.fromString("0000aaa1-0000-1000-8001-aabbccddeeff")
        val CHAR_CHALLENGE: UUID = UUID.fromString("0000aaa2-0000-1000-8000-aabbccddeeff")
    }

    @SuppressLint("MissingPermission")
    fun startGattServer() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // ---- Challenge (Write) ----
        val challengeCharacteristic = BluetoothGattCharacteristic(
            CHAR_CHALLENGE,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // ---- Response (Read) ----
        val responseCharacteristic = BluetoothGattCharacteristic(
            CHAR_RESPONSE,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(challengeCharacteristic)
        service.addCharacteristic(responseCharacteristic)
        gattServer?.addService(service)

        Log.i("BLEManager", "GATT-Server gestartet mit Service: $SERVICE_UUID")
    }

    @SuppressLint("MissingPermission")
    fun stopGattServer() {
        gattServer?.close()
        gattServer = null
        Log.i("BLEManager", "GATT-Server gestoppt.")
    }

    @SuppressLint("MissingPermission")
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i("BLEManager", "Verbindungsstatus geändert: $device, state=$newState")
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
            if (characteristic.uuid == CHAR_CHALLENGE) {
                try {
                    // val challenge = value
                    val challenge = value.copyOfRange(0, 16)
                    val rcuId = value.copyOfRange(16, value.size).toString(Charsets.UTF_8)
                    Log.i("BLEManager", "Challenge empfangen von RCU: $rcuId")
                    Log.i(
                        "BLEManager",
                        "Challenge empfangen: ${challenge.joinToString("") { "%02x".format(it) }}"
                    )

                    if (multiMachineMode) {
                        // Wenn eine Maschine ausgewählt wurde, nur noch deren Challenges akzeptieren
                        if (selectedRcuId != null && rcuId != selectedRcuId) {
                            Log.i("BLEManager", "Challenge von $rcuId ignoriert (aktive Maschine: $selectedRcuId).")
                            if (responseNeeded) {
                                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                            }
                            return
                        }

                        pendingChallenges[rcuId] = challenge //Pending Challenges beinhaltet ID -> Challenge
                        onChallengeReceived?.invoke(rcuId)

                        // Starte Sammelmodus, falls nicht schon aktiv
                        if (!collectingChallenges) {
                            collectingChallenges = true
                            Log.i("BLEManager", "Challenge-Sammelphase gestartet (5s).")

                            // Nach 5 Sekunden Sammelphase beenden
                            CoroutineScope(Dispatchers.Default).launch {
                                delay(5000)
                                collectingChallenges = false
                                val collectedIds = pendingChallenges.keys.toList()
                                Log.i("BLEManager", "Sammelphase beendet. ${collectedIds.size} Challenges gesammelt.")
                                onChallengeCollectionFinished?.invoke(collectedIds)  //onChallengeCollectionFinished hat alle empfangenen Ids ohne Challenges
                            }
                        }
                    } else {
                        // Einzelmaschinenmodus: direkt antworten
                        responseValue = processChallenge(challenge)
                        Log.i(
                            "BLEManager",
                            "Response (HMAC): ${responseValue!!.joinToString("") { "%02x".format(it) }}"
                        )
                    }

                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                } catch (e: Exception) {
                    Log.e("BLEManager", "Fehler beim Verarbeiten der Challenge: ${e.message}")
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == CHAR_RESPONSE) {
                val response = responseValue ?: ByteArray(0)
                //Log.i("BLEManager", "Response gelesen: ${String(response)}")
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, response)
                if (responseValue != null) {
                    onAuthenticated?.invoke()
                }
            }
        }
    }

    fun setToken(token: String){
        authToken = token
    }

    private fun processChallenge(challengeBytes: ByteArray): ByteArray {
        //val keyHex = "93c3d96d4af048af94c2f976df66ea38"
        val keyHex = authToken ?: throw IllegalStateException("Kein Token erhalten")
        val keyBytes = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(keyBytes, "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(challengeBytes)  // 32-Byte Digest wie in Python
    }

    fun processSelectedMachine(rcuId: String) {
        selectedRcuId = rcuId
        val challenge = pendingChallenges[rcuId]
        if (challenge != null) {
            responseValue = processChallenge(challenge)
            Log.i("BLEManager", "Response für $rcuId erzeugt.")

            // Jetzt wird die Response bereitgestellt –
            // die RCU liest sie wie bisher über CHAR_RESPONSE.
            // Wenn das geschieht, wird onAuthenticated() ausgelöst.
        } else {
            Log.e("BLEManager", "Keine Challenge für $rcuId gefunden.")
        }
    }

}
