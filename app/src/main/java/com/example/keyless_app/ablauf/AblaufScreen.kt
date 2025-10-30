package com.example.keyless_app.ablauf



import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
import com.example.keyless_app.R



@Composable
fun AblaufScreen(
    viewModel: AblaufViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    // ViewModel-Status als State beobachten
    val status by viewModel.status.collectAsState()
    val machines by viewModel.machines.collectAsState()

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

        Spacer(Modifier.height(40.dp))

        Text("Status: ${status.label}")

        Spacer(Modifier.height(10.dp))

        // Maschinenliste anzeigen (sobald geladen)
        if (machines.isNotEmpty()) {
            Text("Zugewiesene Maschinen:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            machines.forEachIndexed { index, machine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Maschine ${index + 1}: ${machine.name}")
                        Text("Ort: ${machine.location}")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val buttonText = when (status) {
            Status.Error, Status.ErrorToken -> "Retry"
            Status.Idle -> "Start"
            else -> "Loading..."
        }

        Button(onClick = { viewModel.startProcess() }) {
            Text(buttonText)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(onClick = { onLogout() }) {
            Text("Logout")
        }

        // Authentifizierungsdialog anzeigen
        if (status == Status.Authentifiziert) {
            Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Authentifiziert",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Authentifiziert",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }


    }
}

