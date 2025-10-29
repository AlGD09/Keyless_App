package com.example.keyless_app.ablauf



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController

@Composable
fun AblaufScreen(
    viewModel: AblaufViewModel = hiltViewModel()
) {
    // ViewModel-Status als State beobachten
    val status by viewModel.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Keyless Access", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        Text("Status: ${status.label}")

        Spacer(Modifier.height(40.dp))

        val buttonText = when (status) {
            Status.Error, Status.ErrorToken -> "Retry"
            Status.Idle -> "Start"
            else -> "Loading..."
        }

        Button(onClick = { viewModel.startProcess() }) {
            Text(buttonText)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AblaufScreenPreview() {
    com.example.keyless_app.ui.theme.Keyless_AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Keyless Access", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
            Text("Status: Cloud erfolgreich ✅")
            Spacer(Modifier.height(40.dp))
            Button(onClick = {}) { Text("Start") }
            Spacer(Modifier.height(10.dp))
            Button(onClick = {}) { Text("Retry") }
        }
    }
}

