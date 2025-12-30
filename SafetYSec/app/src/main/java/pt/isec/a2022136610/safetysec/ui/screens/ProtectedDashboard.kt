package pt.isec.a2022136610.safetysec.ui.screens

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.ui.components.CountdownAlert
import pt.isec.a2022136610.safetysec.utils.FallDetector
import pt.isec.a2022136610.safetysec.utils.VideoRecordingManager
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@Composable
fun ProtectedDashboard(
    userName: String,
    onHistoryClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val generatedCode by viewModel.generatedCode.collectAsState()
    val showCountdown by viewModel.showCountdown.collectAsState()
    var showCodeDialog by remember { mutableStateOf(false) }
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

    LaunchedEffect(previewView) {
        if (previewView != null) videoManager.startCamera(lifecycleOwner, previewView!!)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).padding(8.dp)) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).also { previewView = it } },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.mode_protected), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.hello_user, userName), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Badge(containerColor = Color.Green) {
                    Text(stringResource(R.string.protection_active), color = Color.Black, modifier = Modifier.padding(4.dp))
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.generateAssociationCode() }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_associate))
                }
                OutlinedButton(onClick = onHistoryClick) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_history))
                }
            }
        }

        if (showCodeDialog && generatedCode != null) {
            AlertDialog(
                onDismissRequest = { showCodeDialog = false },
                title = { Text(stringResource(R.string.code_dialog_title)) },
                text = { Text(generatedCode!!, style = MaterialTheme.typography.displayMedium) },
                confirmButton = { TextButton(onClick = { showCodeDialog = false }) { Text(stringResource(R.string.btn_close)) } }
            )
        }

        if (showCountdown) {
            CountdownAlert(
                reason = "Alert in progress...",
                onCancel = { pin -> viewModel.verifyPinAndCancel(pin) },
                onTimeout = {
                    viewModel.executeFinalAlert()
                    Toast.makeText(context, "ALERT SENT! RECORDING...", Toast.LENGTH_LONG).show()
                    videoManager.startRecording30Seconds { videoUrl ->
                        viewModel.executeFinalAlert(videoUrl)
                    }
                }
            )
        }
    }
}