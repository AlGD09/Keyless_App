package com.example.keyless_app

import android.content.Context
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.keyless_app.ablauf.AblaufScreen
import com.example.keyless_app.register.RegisterScreen
import com.example.keyless_app.ui.theme.Keyless_AppTheme
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }
        setContent {
            Keyless_AppTheme {
                val context = this
                var isRegistered by remember { mutableStateOf(isUserRegistered(context)) }

                if (isRegistered) {
                    AblaufScreen(onLogout = {
                        clearCredentials(context)
                        isRegistered = false
                    })
                } else {
                    RegisterScreen(onRegistered = { isRegistered = true })
                }
            }
        }
    }

    private fun isUserRegistered(context: Context): Boolean {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", null)
        val secretHash = prefs.getString("secretHash", null)
        return !userName.isNullOrBlank() && !secretHash.isNullOrBlank()
    }

    private fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }



}
