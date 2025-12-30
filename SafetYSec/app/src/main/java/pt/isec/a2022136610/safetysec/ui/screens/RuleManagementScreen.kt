package pt.isec.a2022136610.safetysec.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
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

    LaunchedEffect(protectedId) {
        viewModel.loadRulesForUser(protectedId)
    }

    val rules by viewModel.selectedUserRules.collectAsState()

    var speedLimit by remember { mutableStateOf("") }
    var inactivityLimit by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("20:00") }

    // --- WHEEL PICKER STATE ---
    var showWheelPicker by remember { mutableStateOf(false) }
    var isSelectingStartTime by remember { mutableStateOf(true) }

    // Auto-fill
    LaunchedEffect(rules) {
        val speedRule = rules.find { it.type == RuleType.MAX_SPEED }
        if (speedRule != null) {
            speedLimit = speedRule.maxSpeedKmh?.toString() ?: ""
        }

        val inactivityRule = rules.find { it.type == RuleType.INACTIVITY }
        if (inactivityRule != null) {
            inactivityLimit = inactivityRule.inactivityTimeMinutes?.toString() ?: ""
            startTime = inactivityRule.startTime ?: "09:00"
            endTime = inactivityRule.endTime ?: "20:00"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_rules)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {

            // --- SPEED RULE ---
            Text("Speed Control", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = speedLimit,
                onValueChange = { speedLimit = it },
                label = { Text(stringResource(R.string.rule_speed)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // --- INACTIVITY RULE ---
            Text("Inactivity Monitor", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = inactivityLimit,
                onValueChange = { inactivityLimit = it },
                label = { Text(stringResource(R.string.rule_inactivity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // --- TIME SELECTION (Optional) ---
            Text("Active Hours (Optional):", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Start Time
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.time_start)) },
                    modifier = Modifier.weight(1f).clickable {
                        isSelectingStartTime = true
                        showWheelPicker = true
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isSelectingStartTime = true; showWheelPicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        }
                    }
                )

                // End Time
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.time_end)) },
                    modifier = Modifier.weight(1f).clickable {
                        isSelectingStartTime = false
                        showWheelPicker = true
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isSelectingStartTime = false; showWheelPicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- GLOBAL SET RULES BUTTON ---
            Button(
                onClick = {
                    // 1. Save Speed Rule
                    val existingSpeedRule = rules.find { it.type == RuleType.MAX_SPEED }
                    val newSpeedRule = SafetyRule(
                        id = existingSpeedRule?.id ?: "",
                        protectedId = protectedId,
                        type = RuleType.MAX_SPEED,
                        name = "Max Speed Limit",
                        maxSpeedKmh = speedLimit.toDoubleOrNull() ?: 120.0,
                        isActive = true,
                        startTime = startTime,
                        endTime = endTime
                    )
                    viewModel.saveRule(newSpeedRule)

                    // 2. Save Inactivity Rule
                    val existingInactivityRule = rules.find { it.type == RuleType.INACTIVITY }
                    val newInactivityRule = SafetyRule(
                        id = existingInactivityRule?.id ?: "",
                        protectedId = protectedId,
                        type = RuleType.INACTIVITY,
                        name = "Inactivity Check",
                        inactivityTimeMinutes = inactivityLimit.toIntOrNull() ?: 30,
                        isActive = true,
                        startTime = startTime,
                        endTime = endTime
                    )
                    viewModel.saveRule(newInactivityRule)

                    Toast.makeText(context, "Rules Updated Successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Set Rules")
            }
        }
    }

    if (showWheelPicker) {
        val initialTime = if (isSelectingStartTime) startTime else endTime
        val parts = initialTime.split(":")
        val initH = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initM = parts.getOrNull(1)?.toIntOrNull() ?: 0

        WheelTimePickerDialog(
            initialHour = initH,
            initialMinute = initM,
            onCancel = { showWheelPicker = false },
            onConfirm = { h, m ->
                val formatted = String.format("%02d:%02d", h, m)
                if (isSelectingStartTime) startTime = formatted else endTime = formatted
                showWheelPicker = false
            }
        )
    }
}

// --- CUSTOM WHEEL PICKER DIALOG ---
@Composable
fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onCancel: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    // We use a temporary state to hold values while scrolling
    var currentHour by remember { mutableIntStateOf(initialHour) }
    var currentMinute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { onConfirm(currentHour, currentMinute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Time", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                // Using AndroidView to wrap the native TimePicker in Spinner Mode
                AndroidView(
                    factory = { context ->
                        // Theme_Holo_Light_Dialog_NoActionBar forces the "Wheel/Spinner" style
                        val contextThemeWrapper = ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog_NoActionBar)
                        TimePicker(contextThemeWrapper).apply {
                            setIs24HourView(true)
                            hour = initialHour
                            minute = initialMinute
                            setOnTimeChangedListener { _, h, m ->
                                currentHour = h
                                currentMinute = m
                            }
                        }
                    },
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    )
}