package pt.isec.a2022136610.safetysec.ui.screens

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.ui.components.CountdownAlert
import pt.isec.a2022136610.safetysec.utils.FallDetector
import pt.isec.a2022136610.safetysec.utils.VideoRecordingManager
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@Composable
fun ProtectedDashboard(
    userName: String,
    onHistoryClick: () -> Unit,
    onActiveRulesClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val generatedCode by viewModel.generatedCode.collectAsState()
    val showCountdown by viewModel.showCountdown.collectAsState()
    val monitors by viewModel.associatedMonitors.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingRules by viewModel.pendingRules.collectAsState()

    var showCodeDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showMonitorsDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var newPinInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoManager = remember { VideoRecordingManager(context) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val fallDetector = remember { FallDetector(context) }

    DisposableEffect(Unit) {
        fallDetector.startListening(
            onFall = { viewModel.handleFallDetected() },
            onAccident = { viewModel.handleAccidentDetected() }
        )
        onDispose { fallDetector.stopListening() }
    }

    LaunchedEffect(generatedCode) { if (generatedCode != null) showCodeDialog = true }
    LaunchedEffect(previewView) { if (previewView != null) videoManager.startCamera(lifecycleOwner, previewView!!) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val cameraModifier = if (isRecording) {
            Modifier.fillMaxSize().border(4.dp, Color.White).padding(4.dp).align(Alignment.Center)
        } else {
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(1.dp).alpha(0f)
        }

        Box(modifier = cameraModifier) {
            AndroidView(factory = { ctx -> PreviewView(ctx).also { previewView = it } }, modifier = Modifier.fillMaxSize())
        }

        if (!isRecording) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.mode_protected), style = MaterialTheme.typography.labelMedium)
                                Text(stringResource(R.string.hello_user, userName), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Badge(containerColor = Color.Green) { Text(stringResource(R.string.protection_active), color = Color.Black, modifier = Modifier.padding(4.dp)) }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.startPanicButtonSequence() },
                            modifier = Modifier.size(140.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "SOS", modifier = Modifier.size(48.dp))
                        }
                        Text(stringResource(R.string.sos_instruction), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (pendingRules.isNotEmpty()) {
                            Button(onClick = { showRulesDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) {
                                Icon(Icons.Default.Assignment, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.new_rules_badge) + " (${pendingRules.size})")
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        OutlinedButton(onClick = onActiveRulesClick, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Assignment, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_active_rules))
                        }
                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(onClick = { viewModel.generateAssociationCode() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_associate))
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { showMonitorsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Group, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_my_monitors))
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onHistoryClick, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_history))
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { showPinDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_change_pin))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.mode_protected), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.hello_user, userName), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Badge(containerColor = Color.Green) { Text(stringResource(R.string.protection_active), color = Color.Black, modifier = Modifier.padding(4.dp)) }
                        }
                    }

                    if (pendingRules.isNotEmpty()) {
                        Button(
                            onClick = { showRulesDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Icon(Icons.Default.Assignment, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.new_rules_badge) + " (${pendingRules.size})")
                        }
                    }

                    Button(
                        onClick = { viewModel.startPanicButtonSequence() },
                        modifier = Modifier.size(200.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "SOS", modifier = Modifier.size(48.dp))
                            Text(stringResource(R.string.sos_button), style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                    Text(stringResource(R.string.sos_instruction), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onActiveRulesClick, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Assignment, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_active_rules))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.generateAssociationCode() }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_associate))
                            }
                            OutlinedButton(onClick = { showMonitorsDialog = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Group, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_my_monitors))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onHistoryClick, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_history))
                            }
                            OutlinedButton(onClick = { showPinDialog = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_change_pin))
                            }
                        }
                    }
                }
            }
        } else {
            Text(stringResource(R.string.rec_emergency_msg), color = Color.Red, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp))
        }

        if (showRulesDialog) {
            AlertDialog(
                onDismissRequest = { showRulesDialog = false },
                title = { Text(stringResource(R.string.title_rule_requests)) },
                text = {
                    if (pendingRules.isEmpty()) {
                        Text(stringResource(R.string.no_pending_rules))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(pendingRules) { rule ->
                                val monitorName = monitors.find { it.id == rule.monitorId }?.name ?: "Unknown"

                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(stringResource(R.string.msg_rule_request, rule.name), fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.label_requested_by, monitorName), style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                        Spacer(Modifier.height(4.dp))
                                        val detail = when (rule.type) {
                                            RuleType.MAX_SPEED -> stringResource(R.string.details_speed, rule.maxSpeedKmh.toString())
                                            RuleType.INACTIVITY -> stringResource(R.string.details_inactivity, rule.inactivityTimeMinutes.toString())
                                            RuleType.GEOFENCING -> stringResource(R.string.details_geo, rule.geofenceRadiusMeters.toString())
                                            else -> "Details: N/A"
                                        }
                                        val timeWindow = if(rule.startTime != null && rule.endTime != null) {
                                            stringResource(R.string.details_time_window, rule.startTime!!, rule.endTime!!)
                                        } else ""
                                        Text(detail + timeWindow, style = MaterialTheme.typography.bodyMedium)

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { viewModel.rejectRule(rule.id) }) {
                                                Text(stringResource(R.string.btn_reject), color = Color.Red)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(onClick = { viewModel.approveRule(rule.id) }) {
                                                Text(stringResource(R.string.btn_approve))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showRulesDialog = false }) { Text(stringResource(R.string.btn_close)) } }
            )
        }

        if (showCodeDialog && generatedCode != null) {
            AlertDialog(onDismissRequest = { showCodeDialog = false }, title = { Text(stringResource(R.string.code_dialog_title)) }, text = { Text(generatedCode!!, style = MaterialTheme.typography.displayMedium) }, confirmButton = { TextButton(onClick = { showCodeDialog = false }) { Text(stringResource(R.string.btn_close)) } })
        }
        if (showMonitorsDialog && currentUser != null) {
            AlertDialog(onDismissRequest = { showMonitorsDialog = false }, title = { Text(stringResource(R.string.title_my_monitors)) }, text = { if (monitors.isEmpty()) Text(stringResource(R.string.no_monitors)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(monitors) { monitor -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(monitor.name, style = MaterialTheme.typography.bodyLarge) }; IconButton(onClick = { viewModel.disassociateUsers(monitorId = monitor.id, protectedId = currentUser!!.id) }) { Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red) } } } } }, confirmButton = { TextButton(onClick = { showMonitorsDialog = false }) { Text(stringResource(R.string.btn_close)) } })
        }
        if (showPinDialog) {
            AlertDialog(onDismissRequest = { showPinDialog = false }, title = { Text(stringResource(R.string.title_update_pin)) }, text = { Column { Text(stringResource(R.string.prompt_new_pin)); OutlinedTextField(value = newPinInput, onValueChange = { if (it.length <= 4) newPinInput = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true) } }, confirmButton = { Button(onClick = { if (newPinInput.length == 4) { viewModel.updateCancelPin(newPinInput); Toast.makeText(context, context.getString(R.string.msg_pin_updated), Toast.LENGTH_SHORT).show(); showPinDialog = false; newPinInput = "" } else { Toast.makeText(context, context.getString(R.string.err_pin_digits), Toast.LENGTH_SHORT).show() } }) { Text(stringResource(R.string.btn_update)) } }, dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(stringResource(R.string.btn_cancel)) } })
        }
        if (showCountdown) {
            CountdownAlert(reason = stringResource(R.string.alert_progress), onCancel = { pin -> viewModel.verifyPinAndCancel(pin) }, onTimeout = { viewModel.executeFinalAlert(); Toast.makeText(context, context.getString(R.string.alert_sent_rec), Toast.LENGTH_LONG).show(); videoManager.startRecording30Seconds(onRecordingStart = { isRecording = true }, onRecordingEnd = { isRecording = false }, onVideoUploaded = { videoUrl -> viewModel.executeFinalAlert(videoUrl) }) })
        }
    }
}