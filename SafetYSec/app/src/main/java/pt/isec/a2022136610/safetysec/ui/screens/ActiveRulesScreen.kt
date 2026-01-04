package pt.isec.a2022136610.safetysec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRulesScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val myRules by viewModel.myActiveRules.collectAsState()
    val monitors by viewModel.associatedMonitors.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_active_rules)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (myRules.isEmpty()) {
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.msg_no_active_rules), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(myRules) { rule ->
                    // Find monitor name from associated list
                    val monitorName = monitors.find { it.id == rule.monitorId }?.name ?: "Unknown"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Warning, null) },
                            headlineContent = {
                                Column {
                                    Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.label_set_by, monitorName), style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                }
                            },
                            supportingContent = {
                                val details = when (rule.type) {
                                    RuleType.MAX_SPEED -> stringResource(R.string.details_speed, rule.maxSpeedKmh.toString())
                                    RuleType.INACTIVITY -> stringResource(R.string.details_inactivity, rule.inactivityTimeMinutes.toString())
                                    RuleType.GEOFENCING -> stringResource(R.string.details_geo, rule.geofenceRadiusMeters.toString())
                                    else -> stringResource(R.string.status_active)
                                }
                                val time = if(rule.startTime != null && rule.endTime != null) {
                                    stringResource(R.string.details_time_window, rule.startTime!!, rule.endTime!!)
                                } else ""
                                Text(details + time)
                            },
                            trailingContent = {
                                Button(
                                    onClick = { viewModel.revokeRule(rule.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_revoke))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}