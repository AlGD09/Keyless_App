package com.example.keyless_app.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CloudClient – verwaltet die Kommunikation mit der Cloud.
 * Zunächst eine simulierte (Mock-) Implementierung.
 */
class CloudClient @Inject constructor() {

    private val baseUrl = "http://192.168.0.100:8080/"   // ← IP hier einsetzen

    private val api: CloudApi

    init {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(CloudApi::class.java)
    }

    suspend fun fetchToken(): String? {
        val userName = "Admin"           // später dynamisch
        val deviceId = "bd45e75870af93c2"    // dein BLE-Device-ID
        val secretHash = "cc03e747a6afbbcbf8be7668acfebee5"   // Dummy-Wert

        return try {
            val response = api.requestToken(TokenRequest(userName, deviceId, secretHash))
            if (response.isSuccessful) {
                val token = response.body()?.token
                Log.i("CloudClient", "Token erfolgreich empfangen: $token")
                token
            } else {
                Log.e("CloudClient", "Fehler: ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("CloudClient", "Verbindungsfehler: ${e.message}")
            null
        }
    }
}
