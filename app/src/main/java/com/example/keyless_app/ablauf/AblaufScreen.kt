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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val unlockedMachines by viewModel.unlockedMachines.collectAsState()
    val showUserInfoDialog by viewModel.showUserInfoDialog.collectAsState()
    val pendingMachines by viewModel.pendingMachines.collectAsState()
    val authenticatedMachine by viewModel.authenticatedMachine.collectAsState()
    val lockedMachine by viewModel.lockedMachine.collectAsState()


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
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.infosymbol),
                contentDescription = "Info",
                tint = Color.White,
                modifier = Modifier
                    .size(25.dp)
                    .clickable { viewModel.toggleUserInfoDialog() }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logouticon),
                    contentDescription = "Logout",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onLogout() }
                )
                Spacer(modifier = Modifier.width(0.5.dp))
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLogout() }
                )
            }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Bild je nach Maschinentyp auswählen
                            val imageRes = when {
                                machine.name.contains("Bagger", ignoreCase = true) -> R.drawable.baggersymbol
                                machine.name.contains("Kuka", ignoreCase = true) -> R.drawable.kukasymbol
                                machine.name.contains("Walze", ignoreCase = true) -> R.drawable.walzesymbol
                                else -> R.drawable.maschinesymbol
                            }
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = "Maschinen-Symbol",
                                modifier = Modifier
                                    .size(48.dp)
                                //.padding(start = 2.dp)
                            )
                            Column {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("${machine.name} ")
                                        }
                                    }
                                )
                                Text("Standort: ${machine.location}")
                            }

                            val changeStatus = when {
                                machine.status.contains("Remote", ignoreCase = true) -> "Ferngest."
                                else -> machine.status
                            }

                            val displayStatus = changeStatus.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }

                            val statusColor = when {
                                machine.status == "idle" -> Color(0xFF16A34A) // green-600
                                machine.status == "offline" -> Color(0xFF4B5563) // gray-600
                                machine.status == "operational" -> Color(0xFFC2410C) // orange-700
                                machine.status.contains("Remote") -> Color(0xFFC2410C) // orange-700
                                else -> Color(0xFF6B7280) // gray-500
                            }


                            Text(
                                text = displayStatus,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = statusColor,
                                modifier = Modifier
                                    .width(90.dp)
                                    .wrapContentHeight()
                                    .padding(start = 1.dp)
                                    .align(Alignment.CenterVertically),
                                textAlign = TextAlign.Center     // ❗ Zentriert
                            )


                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            val buttonText: String
            val buttonColor: Color

            when (status) {
                Status.Error, Status.ErrorToken, Status.LockError -> {
                    buttonText = "Retry"
                    buttonColor = Color(0xFFB71C1C) // Rot
                }
                Status.Idle, Status.Locked -> {
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

            if (unlockedMachines.isNotEmpty() && machines.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Aktive Maschinen:", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier
                    .height(8.dp)
                )

                unlockedMachines.forEachIndexed { index, unlocked ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF002B49), // Dunkelblau
                            contentColor = Color.White // Textfarbe in der Card
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Bild je nach Maschinentyp auswählen
                            val imageRes = when {
                                unlocked.name.contains("Bagger", ignoreCase = true) -> R.drawable.baggersymbol
                                unlocked.name.contains("Kuka", ignoreCase = true) -> R.drawable.kukasymbol
                                unlocked.name.contains("Walze", ignoreCase = true) -> R.drawable.walzesymbol
                                else -> R.drawable.maschinesymbol
                            }
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = "Maschinen-Symbol",
                                modifier = Modifier
                                    .size(48.dp)
                                //.padding(start = 2.dp)
                            )
                            Column {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("${unlocked.name} ")
                                        }
                                    }
                                )
                                Text("Standort: ${unlocked.location}")
                            }


                            Button(
                                onClick = {
                                    viewModel.lockMachine(unlocked.rcuId)
                                    // lockMachine = unlocked.name
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF002B49),
                                    contentColor = Color.White // Textfarbe
                                )
                            ) {
                                Text(
                                    text = "Stop",
                                    fontSize = 18.sp,
                                    color = Color(0xFFDA0000)
                                )

                            }

                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))



            // Authentifizierungsdialog anzeigen
            if (status == Status.Authentifiziert) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                            val custom20 = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Maschine freigegeben",
                                style = custom20
                            )
                            Spacer(Modifier.height(16.dp))
                            authenticatedMachine?.let { id ->
                                val name = machines.firstOrNull { it.rcuId == id }?.name ?: id
                                Text(
                                    text = "$name ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                           Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Gehen Sie näher an die Maschine, um sie zu entriegeln",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Entsperrungsdialog anzeigen
            if (status == Status.Entsperrt) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Authentifiziert",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Entriegelt",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(16.dp))
                            authenticatedMachine?.let { id ->
                                val name = machines.firstOrNull { it.rcuId == id }?.name ?: id
                                Text(
                                    text = "Maschine: $name",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Verriegelungsdialog anzeigen
            if (status == Status.Locked) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Verriegelt",
                                tint = Color.Red,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Verriegelt",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(16.dp))

                            val name = lockedMachine
                            Text(
                                text = "Maschine: $name",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        }
                    }
                }
            }

            // Timeout Verriegelungsdialog anzeigen
            if (status == Status.LockTimeout) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Timeout",
                                tint = Color(0xFFFFA500),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Maschinenfehler",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(16.dp))
                            val name = lockedMachine
                            Text(
                                text = buildAnnotatedString {
                                    append("Maschine ")

                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(name)
                                    }

                                    append(" ist nicht erreichbar.\n")
                                    append("Bitte verriegeln Sie die Maschine manuell, falls sie noch aktiv ist.")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Verriegelungsdialog anzeigen
            if (status == Status.LockError) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                                imageVector = Icons.Default.Error,
                                contentDescription = "Timeout",
                                tint = Color.Red,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Verbindungsfehler",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(16.dp))
                            val name = lockedMachine
                            Text(
                                text = "Bitte überprüfen Sie ihre Verbindung zur Keyless Cloud und versuchen Sie es erneut",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Verriegelungsdialog anzeigen
            if (status == Status.LockDeprecated) {
                Dialog(onDismissRequest = { /* bleibt kurz sichtbar */ }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 8.dp,
                        color = Color.White,
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
                                imageVector = Icons.Default.Error,
                                contentDescription = "Timeout",
                                tint = Color.Red,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Veralteter Befehl",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(16.dp))
                            val name = lockedMachine
                            Text(
                                text = "Diese Anfrage ist nicht mehr gültig.\n" +
                                "Maschine $name wird vermutlich von einem anderen Gerät gesteuert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Auswahl-Dialog für mehrere Maschinen ---
            if (status == Status.Auswahl && pendingMachines.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { /* bleibt bewusst offen */ },
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
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ){
                                Text(
                                    text = "Welche Maschine soll entsperrt werden?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 20.sp,
                                    color = Color(0xFF495E6E),
                                    modifier = Modifier
                                        .padding(top = 16.dp, start = 20.dp)
                                        .align(Alignment.CenterHorizontally),
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    userScrollEnabled = false // optional, wenn wenige Maschinen
                                ) {
                                    items(pendingMachines, key = { it }) { rcuId ->
                                        val machine = machines.firstOrNull { it.rcuId == rcuId }
                                        val displayName = machine?.name ?: rcuId

                                        val imageRes = when {
                                            displayName.contains("Bagger", ignoreCase = true) -> R.drawable.baggersymbol
                                            displayName.contains("Kuka", ignoreCase = true) -> R.drawable.kukasymbol
                                            displayName.contains("Walze", ignoreCase = true) -> R.drawable.walzesymbol
                                            else -> R.drawable.maschinesymbol
                                        }

                                        Button(
                                            onClick = { viewModel.handleMachineSelection(rcuId) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF002B49),
                                                contentColor = Color.White // Textfarbe
                                            ),
                                            modifier = Modifier
                                                .aspectRatio(1f) // quadratische Form
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = imageRes),
                                                    contentDescription = "Maschinen-Symbol",
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .padding(bottom = 6.dp)
                                                )
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                            }

                        }
                    },
                    containerColor = Color.Transparent
                )
            }

            if (showUserInfoDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleUserInfoDialog() },
                    confirmButton = {}, // Kein eigener Buttonbereich
                    text = {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 8.dp,
                            color = Color(0xFF5E7F94),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // --- Textbereich (links ausgerichtet) ---
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "Angemeldeter Benutzer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = viewModel.getUserName(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Gerät-ID",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = viewModel.getDeviceId(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // --- "Schließen" unten rechts ---
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Schließen",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clickable { viewModel.toggleUserInfoDialog() }
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