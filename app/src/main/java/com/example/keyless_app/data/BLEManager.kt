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
import javax.inject.Inject
import java.util.UUID
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

        // Manufacturer Data – Company ID 0xFFFF, Device ID bd45e75870af93c2
        val manufacturerId = 0xFFFF
        val deviceIdBytes = byteArrayOf(
            0xBD.toByte(), 0x45.toByte(), 0xE7.toByte(), 0x58.toByte(),
            0x70.toByte(), 0xAF.toByte(), 0x93.toByte(), 0xC2.toByte()
        )

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
                val challenge = value
                Log.i("BLEManager", "Challenge empfangen: ${challenge.joinToString("") { "%02x".format(it) }}")

                // Beispielhafte Berechnung der Antwort
                responseValue = processChallenge(challenge)
                Log.i("BLEManager", "Response (HMAC) = ${responseValue!!.joinToString("") { "%02x".format(it) }}")

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
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

}
