package com.example.keyless_app.register

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val context = LocalContext.current

    var userName by remember { mutableStateOf("") }
    var secretHash by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registrierung", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Benutzername") }
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = secretHash,
            onValueChange = { secretHash = it },
            label = { Text("Secret Hash") }
        )

        Spacer(Modifier.height(30.dp))

        Button(onClick = {
            saveCredentials(context, userName, secretHash)
            onRegistered()
        }) {
            Text("Speichern")
        }
    }
}

fun saveCredentials(context: Context, userName: String, secretHash: String) {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("userName", userName)
        .putString("secretHash", secretHash)
        .apply()
}
