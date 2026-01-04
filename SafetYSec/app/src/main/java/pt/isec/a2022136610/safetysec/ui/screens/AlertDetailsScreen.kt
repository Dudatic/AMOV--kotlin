package pt.isec.a2022136610.safetysec.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsScreen(
    navController: NavController,
    alertId: String,
    viewModel: AuthViewModel = viewModel()
) {
    val alert by viewModel.selectedAlert.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(alertId) { viewModel.getAlert(alertId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alert_details_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            if (alert != null) {
                val date = alert!!.timestamp.toDate()
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)
                val isResolved = alert!!.status == "RESOLVED"

                if (isLandscape) {
                    // --- LANDSCAPE LAYOUT ---
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Alert Info & Map Button
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isResolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            null,
                                            tint = if (isResolved) Color.Green else Color.Red
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(alert!!.ruleType.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.alert_status_label, alert!!.status),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isResolved) Color.Green else Color.Red
                                    )
                                    Text(stringResource(R.string.alert_time_label, format))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        stringResource(
                                            R.string.alert_reason_label,
                                            alert!!.cancelReason ?: stringResource(R.string.alert_reason_na)
                                        ),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    alert!!.location?.let { loc ->
                                        val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(Alert)")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = alert!!.location != null
                            ) {
                                Icon(Icons.Default.Map, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_open_maps))
                            }
                        }

                        // Right Column: Video & Resolution Actions
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (alert!!.videoUrl != null) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert!!.videoUrl))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.PlayCircleFilled, null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_watch_video), color = Color.White)
                                }
                                Spacer(Modifier.height(24.dp))
                            } else {
                                Text(stringResource(R.string.msg_no_video), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(24.dp))
                            }

                            if (!isResolved) {
                                Button(
                                    onClick = { viewModel.dismissAlert(alert!!.id) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text(stringResource(R.string.btn_mark_resolved))
                                }
                            } else {
                                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.btn_resolved))
                                }
                            }
                        }
                    }
                } else {
                    // --- PORTRAIT LAYOUT ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isResolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        null,
                                        tint = if (isResolved) Color.Green else Color.Red
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(alert!!.ruleType.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.alert_status_label, alert!!.status),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isResolved) Color.Green else Color.Red
                                )
                                Text(stringResource(R.string.alert_time_label, format))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(
                                        R.string.alert_reason_label,
                                        alert!!.cancelReason ?: stringResource(R.string.alert_reason_na)
                                    ),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        if (alert!!.location != null) {
                            Button(
                                onClick = {
                                    val loc = alert!!.location!!
                                    val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(Alert)")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Map, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_view_map))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (alert!!.videoUrl != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert!!.videoUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                            ) {
                                Icon(Icons.Default.PlayCircleFilled, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_watch_video), color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        if (!isResolved) {
                            Button(
                                onClick = { viewModel.dismissAlert(alert!!.id) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text(stringResource(R.string.btn_mark_resolved))
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}