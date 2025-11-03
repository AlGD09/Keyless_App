package com.example.keyless_app.register

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyless_app.R
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontStyle

@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("") }
    var secretHash by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.infosymbol),
                contentDescription = "Info",
                tint = Color.Gray,
                modifier = Modifier
                    .size(25.dp)
                    .clickable { showInfoDialog = true }
            )
        }
        //  Hauptinhalt (zentriert)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.keylesslogo2),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 0.dp)
            )

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

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Registrierung",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, bottom = 5.dp)
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

        if (showInfoDialog) {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                confirmButton = {},
                text = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 8.dp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            //horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Gerät-ID",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "Schließen",
                                    color = Color(0xFF5E7F94),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { showInfoDialog = false }
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                    }
                },
                containerColor = Color.Transparent
            )
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
