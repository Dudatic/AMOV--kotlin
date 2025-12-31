package pt.isec.a2022136610.safetysec.ui.screens

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.viewmodel.AuthState
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MonitorDashboard(
    userName: String,
    onUserClick: (String) -> Unit,
    onGeofenceClick: (String) -> Unit,
    onRuleManageClick: (String) -> Unit,
    onAlertClick: (String) -> Unit, // New callback
    viewModel: AuthViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val associatedUsers by viewModel.associatedUsers.collectAsState()
    val monitorAlerts by viewModel.monitorAlerts.collectAsState()

    val context = LocalContext.current

    // Statistics
    val activeUsersCount = associatedUsers.size
    val activeAlertsCount = monitorAlerts.count { it.status == "ACTIVE" }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success && showDialog) {
            val msg = context.getString(R.string.associate_new) + " Success!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            showDialog = false
            codeInput = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- STATISTICS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.statistics_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeUsersCount.toString(), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.stat_tracked_users), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeAlertsCount.toString(), style = MaterialTheme.typography.headlineMedium, color = Color.Red)
                        Text(stringResource(R.string.stat_active_alerts), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- RECENT ALERTS SECTION ---
        Text("Recent Alerts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

        if (monitorAlerts.isEmpty()) {
            Text("No active alerts", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(8.dp))
        } else {
            LazyColumn(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                items(monitorAlerts) { alert ->
                    val color = if (alert.status == "ACTIVE") Color(0xFFFFEBEE) else Color.White
                    val iconColor = if (alert.status == "ACTIVE") Color.Red else Color.Gray

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onAlertClick(alert.id) },
                        colors = CardDefaults.cardColors(containerColor = color)
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Warning, contentDescription = null, tint = iconColor) },
                            headlineContent = { Text(alert.ruleType.name, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                val date = alert.timestamp.toDate()
                                val format = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                                Text("${format.format(date)} | ${alert.cancelReason ?: "Emergency"}")
                            },
                            trailingContent = {
                                if (alert.status == "ACTIVE") {
                                    Badge(containerColor = Color.Red) { Text("ACTIVE", color = Color.White) }
                                } else {
                                    Text(alert.status, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                }
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- PROTEGES LIST ---
        Text(
            stringResource(R.string.my_proteges),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp)
        )

        if (associatedUsers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_proteges), color = MaterialTheme.colorScheme.secondary)
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
                                    Text(stringResource(R.string.location_ok), color = MaterialTheme.colorScheme.primary)
                                else
                                    Text(stringResource(R.string.no_location))
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onRuleManageClick(user.id) }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Rules")
                                    }
                                    IconButton(onClick = { onGeofenceClick(user.id) }) {
                                        Icon(Icons.Default.Security, contentDescription = "Geofence")
                                    }
                                    IconButton(onClick = { onUserClick(user.id) }) {
                                        Icon(Icons.Default.LocationOn, contentDescription = "Map")
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
            Text(stringResource(R.string.associate_new))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.code_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_code_instruction))
                    OutlinedTextField(
                        value = codeInput, onValueChange = { codeInput = it },
                        label = { Text("Code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = { Button(onClick = { viewModel.connectWithProtege(codeInput) }) { Text(stringResource(R.string.btn_ok)) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}