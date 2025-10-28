package com.example.keyless_app.data


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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

    /**
     * Startet den BLE-Authentifizierungsprozess.
     * @param token Token, das von der Cloud erhalten wurde.
     * @return true, wenn Authentifizierung erfolgreich, false sonst.
     */

    suspend fun startAdvertisingForDuration(durationMs: Long = 10_000L) {
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
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")))
            .addServiceData(
                ParcelUuid(UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")),
                "KEYLESS".toByteArray()
            )
            .setIncludeDeviceName(false)
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
    suspend fun startAuthentication(token: String): Boolean {
        // Simuliere die Zeit für BLE-Advertising und Authentifizierung
        delay(2000)

        // Logikplatzhalter: prüfe Token (in Zukunft wird hier echtes BLE kommen)
        return token.isNotEmpty() && token.startsWith("mock")
    }

    /**
     * (Später) Startet BLE Advertising.
     */
    suspend fun startAdvertising() {
        delay(1000)
        // TODO: hier kommt später echtes BLE-Advertising (BluetoothAdapter + GATT)
    }

    /**
     * (Später) Stoppt Advertising, falls aktiv.
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        Log.i("BLEManager", "BLE-Advertising gestoppt.")
    }
}
