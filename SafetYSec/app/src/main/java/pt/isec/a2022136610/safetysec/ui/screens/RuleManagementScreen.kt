package pt.isec.a2022136610.safetysec.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleStatus
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.model.SafetyRule
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleManagementScreen(
    protectedId: String,
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(protectedId) { viewModel.loadRulesForUser(protectedId) }
    val rules by viewModel.selectedUserRules.collectAsState()

    var speedLimit by remember { mutableStateOf("") }
    var inactivityLimit by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("20:00") }

    var speedStatus by remember { mutableStateOf("") }
    var inactivityStatus by remember { mutableStateOf("") }

    var showWheelPicker by remember { mutableStateOf(false) }
    var isSelectingStartTime by remember { mutableStateOf(true) }

    val statusActive = stringResource(R.string.status_active)
    val statusPending = stringResource(R.string.status_pending)

    LaunchedEffect(rules) {
        val speedRule = rules.find { it.type == RuleType.MAX_SPEED }
        if (speedRule != null) {
            speedLimit = speedRule.maxSpeedKmh?.toString() ?: ""
            speedStatus = if (speedRule.status == RuleStatus.ACTIVE) statusActive else statusPending
        }
        val inactivityRule = rules.find { it.type == RuleType.INACTIVITY }
        if (inactivityRule != null) {
            inactivityLimit = inactivityRule.inactivityTimeMinutes?.toString() ?: ""
            startTime = inactivityRule.startTime ?: "09:00"
            endTime = inactivityRule.endTime ?: "20:00"
            inactivityStatus = if (inactivityRule.status == RuleStatus.ACTIVE) statusActive else statusPending
        }
        // If Geofence exists but not others, try to load time from it too
        if (speedRule == null && inactivityRule == null) {
            val geoRule = rules.find { it.type == RuleType.GEOFENCING }
            if (geoRule != null && geoRule.startTime != null) {
                startTime = geoRule.startTime
                endTime = geoRule.endTime ?: "20:00"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_rules)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back)) }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.speed_control), style = MaterialTheme.typography.titleMedium)
                        if (speedStatus.isNotEmpty()) Text(speedStatus, style = MaterialTheme.typography.labelSmall, color = if (speedStatus == statusActive) Color.Green else Color(0xFFFF9800))
                        OutlinedTextField(value = speedLimit, onValueChange = { speedLimit = it }, label = { Text(stringResource(R.string.rule_speed)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.inactivity_monitor), style = MaterialTheme.typography.titleMedium)
                        if (inactivityStatus.isNotEmpty()) Text(inactivityStatus, style = MaterialTheme.typography.labelSmall, color = if (inactivityStatus == statusActive) Color.Green else Color(0xFFFF9800))
                        OutlinedTextField(value = inactivityLimit, onValueChange = { inactivityLimit = it }, label = { Text(stringResource(R.string.rule_inactivity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.active_hours), style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = startTime, onValueChange = { }, label = { Text(stringResource(R.string.time_start)) }, modifier = Modifier.weight(1f).clickable { isSelectingStartTime = true; showWheelPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant), trailingIcon = { IconButton(onClick = { isSelectingStartTime = true; showWheelPicker = true }) { Icon(Icons.Default.AccessTime, contentDescription = null) } })
                            OutlinedTextField(value = endTime, onValueChange = { }, label = { Text(stringResource(R.string.time_end)) }, modifier = Modifier.weight(1f).clickable { isSelectingStartTime = false; showWheelPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant), trailingIcon = { IconButton(onClick = { isSelectingStartTime = false; showWheelPicker = true }) { Icon(Icons.Default.AccessTime, contentDescription = null) } })
                        }
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { saveRules(rules, protectedId, speedLimit, inactivityLimit, startTime, endTime, viewModel, context) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(stringResource(R.string.set_rules)) }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.speed_control), style = MaterialTheme.typography.titleMedium)
                    if (speedStatus.isNotEmpty()) Text(speedStatus, style = MaterialTheme.typography.labelSmall, color = if (speedStatus == statusActive) Color.Green else Color(0xFFFF9800))
                    OutlinedTextField(value = speedLimit, onValueChange = { speedLimit = it }, label = { Text(stringResource(R.string.rule_speed)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(stringResource(R.string.inactivity_monitor), style = MaterialTheme.typography.titleMedium)
                    if (inactivityStatus.isNotEmpty()) Text(inactivityStatus, style = MaterialTheme.typography.labelSmall, color = if (inactivityStatus == statusActive) Color.Green else Color(0xFFFF9800))
                    OutlinedTextField(value = inactivityLimit, onValueChange = { inactivityLimit = it }, label = { Text(stringResource(R.string.rule_inactivity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.active_hours), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = { }, label = { Text(stringResource(R.string.time_start)) }, modifier = Modifier.weight(1f).clickable { isSelectingStartTime = true; showWheelPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant), trailingIcon = { IconButton(onClick = { isSelectingStartTime = true; showWheelPicker = true }) { Icon(Icons.Default.AccessTime, null) } })
                        OutlinedTextField(value = endTime, onValueChange = { }, label = { Text(stringResource(R.string.time_end)) }, modifier = Modifier.weight(1f).clickable { isSelectingStartTime = false; showWheelPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant), trailingIcon = { IconButton(onClick = { isSelectingStartTime = false; showWheelPicker = true }) { Icon(Icons.Default.AccessTime, null) } })
                    }
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { saveRules(rules, protectedId, speedLimit, inactivityLimit, startTime, endTime, viewModel, context) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(stringResource(R.string.set_rules)) }
                }
            }
        }
    }

    if (showWheelPicker) {
        val initialTime = if (isSelectingStartTime) startTime else endTime
        val parts = initialTime.split(":")
        val initH = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        WheelTimePickerDialog(initialHour = initH, initialMinute = initM, onCancel = { showWheelPicker = false }, onConfirm = { h, m -> val formatted = String.format("%02d:%02d", h, m); if (isSelectingStartTime) startTime = formatted else endTime = formatted; showWheelPicker = false })
    }
}

fun saveRules(rules: List<SafetyRule>, protectedId: String, speedLimit: String, inactivityLimit: String, startTime: String, endTime: String, viewModel: AuthViewModel, context: Context) {
    // 1. Update/Create Speed Rule
    if (speedLimit.isNotBlank()) {
        val existingSpeedRule = rules.find { it.type == RuleType.MAX_SPEED }
        val newSpeedRule = SafetyRule(
            id = existingSpeedRule?.id ?: "",
            monitorId = existingSpeedRule?.monitorId ?: "",
            protectedId = protectedId,
            type = RuleType.MAX_SPEED,
            name = "Max Speed Limit",
            maxSpeedKmh = speedLimit.toDoubleOrNull() ?: 120.0,
            isActive = true,
            startTime = startTime,
            endTime = endTime,
            status = RuleStatus.PENDING
        )
        viewModel.saveRule(newSpeedRule)
    }

    // 2. Update/Create Inactivity Rule
    if (inactivityLimit.isNotBlank()) {
        val existingInactivityRule = rules.find { it.type == RuleType.INACTIVITY }
        val newInactivityRule = SafetyRule(
            id = existingInactivityRule?.id ?: "",
            monitorId = existingInactivityRule?.monitorId ?: "",
            protectedId = protectedId,
            type = RuleType.INACTIVITY,
            name = "Inactivity Check",
            inactivityTimeMinutes = inactivityLimit.toIntOrNull() ?: 30,
            isActive = true,
            startTime = startTime,
            endTime = endTime,
            status = RuleStatus.PENDING
        )
        viewModel.saveRule(newInactivityRule)
    }

    // 3. Update Geofence Rule if it exists (Apply new time & PENDING status)
    val existingGeoRule = rules.find { it.type == RuleType.GEOFENCING }
    if (existingGeoRule != null) {
        val updatedGeo = existingGeoRule.copy(
            startTime = startTime,
            endTime = endTime,
            status = RuleStatus.PENDING, // Re-request approval for time change
            isActive = true
        )
        viewModel.saveRule(updatedGeo)
    }

    Toast.makeText(context, "Rules Requested! Waiting for approval.", Toast.LENGTH_SHORT).show()
}

@Composable
fun WheelTimePickerDialog(initialHour: Int, initialMinute: Int, onCancel: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var currentHour by remember { mutableIntStateOf(initialHour) }
    var currentMinute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(onDismissRequest = onCancel, confirmButton = { TextButton(onClick = { onConfirm(currentHour, currentMinute) }) { Text("OK") } }, dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("Select Time", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(16.dp)); AndroidView(factory = { context -> val contextThemeWrapper = ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog_NoActionBar); TimePicker(contextThemeWrapper).apply { setIs24HourView(true); hour = initialHour; minute = initialMinute; setOnTimeChangedListener { _, h, m -> currentHour = h; currentMinute = m } } }, modifier = Modifier.wrapContentSize()) } })
}