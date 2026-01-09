package com.example.keyless_app


import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.keyless_app.ablauf.AblaufScreen
import com.example.keyless_app.register.RegisterViewModel
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
                val registerViewModel: RegisterViewModel = hiltViewModel()
                val isRegistered by registerViewModel.isRegistered.collectAsState()

                if (isRegistered) {
                    AblaufScreen(onLogout = {
                        registerViewModel.logout()
                    })
                } else {
                    RegisterScreen(
                        registerViewModel = registerViewModel,
                        onRegistered = { }
                    )
                }
            }
        }
    }



}
