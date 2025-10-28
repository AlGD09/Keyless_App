package com.example.keyless_app.data

import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * CloudClient – verwaltet die Kommunikation mit der Cloud.
 * Zunächst eine simulierte (Mock-) Implementierung.
 */
class CloudClient @Inject constructor() {

    /**
     * Ruft ein Authentifizierungs-Token aus der Cloud ab.
     * @return Token-String (zunächst Dummy)
     */
    suspend fun fetchToken(): String {
        // Simuliere Netzwerkverzögerung
        delay(1500)

        // TODO: später echte REST-Implementierung (z. B. Retrofit)
        return "mock-token-123"
    }
}
