package pt.isec.a2022136610.safetysec.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.model.SafetyAlert
import pt.isec.a2022136610.safetysec.viewmodel.AuthState
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@Composable
fun MonitorDashboard(
    userName: String,
    onUserClick: (String) -> Unit,
    onGeofenceClick: (String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }

    // Variáveis para o Alerta
    var latestAlert by remember { mutableStateOf<SafetyAlert?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val associatedUsers by viewModel.associatedUsers.collectAsState()
    val context = LocalContext.current

    // --- ESCUTA DE ALERTAS EM TEMPO REAL ---
    LaunchedEffect(associatedUsers) {
        val db = FirebaseFirestore.getInstance()

        if (associatedUsers.isNotEmpty()) {
            db.collection("alerts")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener

                    if (snapshots != null && !snapshots.isEmpty) {
                        for (doc in snapshots.documentChanges) {
                            if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                doc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {

                                val alert = doc.document.toObject(SafetyAlert::class.java)
                                val isMyProtege = associatedUsers.any { it.id == alert.protectedId }

                                if (isMyProtege) {
                                    latestAlert = alert
                                    showSosDialog = true
                                }
                            }
                        }
                    } else {
                        showSosDialog = false
                        latestAlert = null
                    }
                }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success && showDialog) {
            Toast.makeText(context, "Protegido associado com sucesso!", Toast.LENGTH_SHORT).show()
            showDialog = false
            codeInput = ""
        }
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Monitor", style = MaterialTheme.typography.labelMedium)
                Text("Olá, $userName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "Meus Protegidos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp)
        )

        if (associatedUsers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Ainda não tem protegidos associados.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(associatedUsers) { user ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.clickable { onUserClick(user.id) }
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                            headlineContent = { Text(user.name) },
                            supportingContent = {
                                if (user.lastLocation != null)
                                    Text("📍 Localização OK", color = MaterialTheme.colorScheme.primary)
                                else
                                    Text("Sem localização")
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onGeofenceClick(user.id) }) {
                                        Icon(Icons.Default.Security, contentDescription = "Cerca")
                                    }
                                    IconButton(onClick = { onUserClick(user.id) }) {
                                        Icon(Icons.Default.LocationOn, contentDescription = "Mapa")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Associar Novo Protegido")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Associar Protegido") },
            text = {
                Column {
                    Text("Insira o código gerado pelo Protegido:")
                    OutlinedTextField(
                        value = codeInput, onValueChange = { codeInput = it },
                        label = { Text("Código") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = { Button(onClick = { viewModel.connectWithProtege(codeInput) }) { Text("Associar") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showSosDialog && latestAlert != null) {

        val ruleType = latestAlert!!.ruleType

        val (alertColor, alertTitle, alertIcon) = when (ruleType) {
            RuleType.GEOFENCING -> Triple(Color(0xFFFF9800), "ALERTA DE ZONA", Icons.Default.Security)
            RuleType.FALL_DETECTION -> Triple(Color(0xFFD500F9), "⚠️ QUEDA DETETADA ⚠️", Icons.Default.Warning) // Roxo/Magenta
            else -> Triple(Color(0xFFFF0000), "PEDIDO DE SOS", Icons.Default.NotificationsActive)
        }

        val bgAlertColor = alertColor.copy(alpha = 0.05f)
        val messageText = latestAlert!!.cancelReason ?: "Alerta sem detalhes."

        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            icon = {
                Icon(alertIcon, contentDescription = null, tint = alertColor, modifier = Modifier.size(48.dp))
            },
            title = { Text(alertTitle, color = alertColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgAlertColor, shape = MaterialTheme.shapes.small)
                        .padding(16.dp)
                ) {
                    Text(messageText, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAlert(latestAlert!!.id)
                        showSosDialog = false
                        onUserClick(latestAlert!!.protectedId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = alertColor)
                ) {
                    Text("VER E RESOLVER")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissAlert(latestAlert!!.id)
                    showSosDialog = false
                }) {
                    Text("IGNORAR", color = alertColor)
                }
            }
        )
    }
}