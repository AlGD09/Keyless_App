package com.example.keyless_app.register

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.keyless_app.R

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
        // --- Kopfzeile mit Logo und Text ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo oben
            Image(
                painter = painterResource(id = R.drawable.keylesslogo2),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 0.dp)
            )

            // Text unterhalb des Logos
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Keyless",
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "App",
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }


        Spacer(Modifier.height(30.dp))

        Text(
            text = "Registrierung",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start =20.dp, bottom = 5.dp)
        )

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
