package com.example.keyless_app.ablauf



import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // --- Animierter Farbverlauf-Hintergrund ---
    val infiniteTransition = rememberInfiniteTransition()

    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFFFFF),
        targetValue = Color(0xFFDA0000),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF002B49),
        targetValue = Color(0xFF3BCCFF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(color1, color2),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { onLogout() }
                .padding(top = 40.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logouticon),
                contentDescription = "Logout",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(0.5.dp))
            Text(
                text = "Logout",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    painter = painterResource(id = R.drawable.keylesslogo3),
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
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 23.sp
                    )
                    Text(
                        text = "App",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(text = "${status.label}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Spacer(Modifier.height(10.dp))

            // Maschinenliste anzeigen (sobald geladen)
            if (machines.isNotEmpty()) {
                Text("Zugewiesene Maschinen:", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))

                machines.forEachIndexed { index, machine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF002B49), // Dunkelblau
                            contentColor = Color.White // Textfarbe in der Card
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Maschine ${index + 1}")
                                    }
                                    append(" - ${machine.name}")
                                }
                            )

                            Text("Standort: ${machine.location}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            val buttonText: String
            val buttonColor: Color

            when (status) {
                Status.Error, Status.ErrorToken -> {
                    buttonText = "Retry"
                    buttonColor = Color(0xFFB71C1C) // Rot
                }
                Status.Idle -> {
                    buttonText = "Start"
                    buttonColor = Color(0xFF2E7D32) // Grün
                }
                else -> {
                    buttonText = "Loading..."
                    buttonColor = Color(0xFFF57C00) // Orange
                }
            }

            Button(
                onClick = { viewModel.startProcess() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White // Textfarbe
                )
            ) {
                if (buttonText == "Loading...") {
                    Box(contentAlignment = Alignment.TopStart) {
                        // Unsichtbarer Platzhalter, damit die Breite fix bleibt
                        Text(
                            text = "Loading...",
                            fontSize = 18.sp,
                            color = Color.Transparent
                        )
                        // Sichtbarer animierter Text
                        LoadingText(baseText = "Loading", intervalMillis = 800)
                    }
                } else {
                    Text(text = buttonText, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(20.dp))



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
}

@Composable
fun LoadingText(baseText: String = "Loading", intervalMillis: Int = 1000) {
    var dotCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            dotCount = (dotCount + 1) % 4
            kotlinx.coroutines.delay(intervalMillis.toLong())
        }
    }

    Text(text = baseText + ".".repeat(dotCount), fontSize = 18.sp)
}