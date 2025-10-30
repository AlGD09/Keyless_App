package com.example.keyless_app.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * CloudClient – verwaltet die Kommunikation mit der Cloud.
 * Zunächst eine simulierte (Mock-) Implementierung.
 */
class CloudClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val baseUrl = "http://10.42.0.1:8080/"   // ← IP hier einsetzen

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
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        //val userName = "Admin"           // später dynamisch
        val deviceId = "bd45e75870af93c2"    // dein BLE-Device-ID
        //val secretHash = "cc03e747a6afbbcbf8be7668acfebee5"   // Dummy-Wert
        val userName = prefs.getString("userName", "") ?: ""
        val secretHash = prefs.getString("secretHash", "") ?: ""

        return try {
            val response = api.requestToken(TokenRequest(userName, deviceId, secretHash))
            if (response.isSuccessful) {
                val token = response.body()?.auth_token
                Log.i("CloudClient", "Token erfolgreich empfangen: $token")
                token
            } else {
                val errorMessage = "Fehler: ${response.code()} ${response.message()}"
                Log.e("CloudClient", errorMessage)
                throw Exception(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("CloudClient", "Verbindungsfehler: ${e.message}")
            throw e
        }
    }
}
