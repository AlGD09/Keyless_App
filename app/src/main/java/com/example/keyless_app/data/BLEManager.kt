package com.example.keyless_app.data

import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * BLEManager – verwaltet die Bluetooth-Kommunikation mit der RCU.
 * Zunächst als Dummy-Implementierung mit simuliertem Ablauf.
 */
class BLEManager @Inject constructor() {

    /**
     * Startet den BLE-Authentifizierungsprozess.
     * @param token Token, das von der Cloud erhalten wurde.
     * @return true, wenn Authentifizierung erfolgreich, false sonst.
     */
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
    fun stopAdvertising() {
        // TODO: später Advertising stoppen
    }
}
