package pt.isec.a2022136610.safetysec.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.model.SafetyAlert
import pt.isec.a2022136610.safetysec.viewmodel.AuthState
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@Composable
fun MonitorDashboard(
    userName: String,
    onUserClick: (String) -> Unit,
    onGeofenceClick: (String) -> Unit,
    onRuleManageClick: (String) -> Unit,
    onAlertClick: (String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var latestAlert by remember { mutableStateOf<SafetyAlert?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }

    // Disassociate state
    var showRemoveDialog by remember { mutableStateOf(false) }
    var userToRemoveId by remember { mutableStateOf<String?>(null) }
    var userToRemoveName by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser.collectAsState()

    val authState by viewModel.authState.collectAsState()
    val associatedUsers by viewModel.associatedUsers.collectAsState()
    val monitorAlerts by viewModel.monitorAlerts.collectAsState()
    val context = LocalContext.current

    val activeUsersCount = associatedUsers.size
    val activeAlertsCount = monitorAlerts.count { it.status == "ACTIVE" }

    LaunchedEffect(associatedUsers) {
        val db = FirebaseFirestore.getInstance()
        if (associatedUsers.isNotEmpty()) {
            db.collection("alerts")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null && !snapshots.isEmpty) {
                        for (doc in snapshots.documentChanges) {
                            val alert = doc.document.toObject(SafetyAlert::class.java)
                            if (associatedUsers.any { it.id == alert.protectedId }) {
                                latestAlert = alert
                                showSosDialog = true
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
            val msg = context.getString(R.string.associate_new) + " Success!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            showDialog = false
            codeInput = ""
        }
    }

    // Components
    val StatsCard = @Composable { modifier: Modifier ->
        Card(
            modifier = modifier.padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.statistics_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
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
    }

    val AlertsList = @Composable { modifier: Modifier ->
        Column(modifier = modifier) {
            Text(stringResource(R.string.recent_alerts), style = MaterialTheme.typography.titleMedium)
            if (monitorAlerts.isEmpty()) {
                Text(stringResource(R.string.no_active_alerts), style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(monitorAlerts) { alert ->
                        val color = if (alert.status == "ACTIVE") Color(0xFFFFEBEE) else Color.White
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onAlertClick(alert.id) },
                            colors = CardDefaults.cardColors(containerColor = color)
                        ) {
                            ListItem(
                                headlineContent = { Text(alert.ruleType.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(alert.cancelReason ?: "Emergency") },
                                trailingContent = { if (alert.status == "ACTIVE") Badge(containerColor = Color.Red) { Text("ACTIVE", color = Color.White) } }
                            )
                        }
                    }
                }
            }
        }
    }

    val ProtegesList = @Composable { modifier: Modifier ->
        Column(modifier = modifier) {
            Text(stringResource(R.string.my_proteges), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
            if (associatedUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_proteges), color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(associatedUsers) { user ->
                        Card(modifier = Modifier.clickable { onUserClick(user.id) }) {
                            ListItem(
                                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                                headlineContent = { Text(user.name) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { onRuleManageClick(user.id) }) { Icon(Icons.Default.Settings, null) }
                                        IconButton(onClick = { onGeofenceClick(user.id) }) { Icon(Icons.Default.Security, null) }
                                        IconButton(onClick = { onUserClick(user.id) }) { Icon(Icons.Default.LocationOn, null) }
                                        // NEW: Remove Link Button
                                        IconButton(onClick = {
                                            userToRemoveId = user.id
                                            userToRemoveName = user.name
                                            showRemoveDialog = true
                                        }) { Icon(Icons.Default.LinkOff, contentDescription = "Remove", tint = Color.Red) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (maxWidth > maxHeight) {
            // --- LANDSCAPE MODE ---
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(stringResource(R.string.monitor_dashboard), style = MaterialTheme.typography.labelSmall)
                                Text(stringResource(R.string.hello_user, userName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        StatsCard(Modifier.weight(1f).fillMaxHeight())
                    }
                    AlertsList(Modifier.weight(1f))
                }
                Column(modifier = Modifier.weight(1f)) {
                    ProtegesList(Modifier.weight(1f))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.associate_new))
                    }
                }
            }
        } else {
            // --- PORTRAIT MODE ---
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.monitor_dashboard), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.hello_user, userName), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                StatsCard(Modifier.fillMaxWidth())
                AlertsList(Modifier.height(150.dp))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ProtegesList(Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.associate_new))
                }
            }
        }
    }

    // Dialogs
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

    if (showRemoveDialog && userToRemoveId != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.title_confirm_remove)) },
            text = { Text(stringResource(R.string.msg_confirm_remove, userToRemoveName)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disassociateUsers(monitorId = currentUser!!.id, protectedId = userToRemoveId!!)
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.btn_remove))
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (showSosDialog && latestAlert != null) {
        val ruleType = latestAlert!!.ruleType
        val (alertColor, alertTitleRes, alertIcon) = when (ruleType) {
            RuleType.GEOFENCING -> Triple(Color(0xFFFF9800), R.string.alert_zone, Icons.Default.Security)
            RuleType.FALL_DETECTION -> Triple(Color(0xFFD500F9), R.string.alert_fall, Icons.Default.Warning)
            else -> Triple(Color(0xFFFF0000), R.string.alert_sos, Icons.Filled.NotificationsActive)
        }
        val bgAlertColor = alertColor.copy(alpha = 0.05f)
        val messageText = latestAlert!!.cancelReason ?: stringResource(R.string.alert_details_missing)

        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            title = { Text(stringResource(alertTitleRes), color = alertColor) },
            text = { Text(messageText, color = Color.Black) },
            icon = { Icon(alertIcon, contentDescription = null, tint = alertColor) },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissAlert(latestAlert!!.id)
                    showSosDialog = false
                    onAlertClick(latestAlert!!.id)
                }, colors = ButtonDefaults.buttonColors(containerColor = alertColor)) {
                    Text(stringResource(R.string.btn_resolve))
                }
            },
            dismissButton = {
                // Modified: Now only closes the dialog, leaving the alert active in DB
                TextButton(onClick = {
                    showSosDialog = false
                }) { Text(stringResource(R.string.btn_ignore), color = alertColor) }
            }
        )
    }
}