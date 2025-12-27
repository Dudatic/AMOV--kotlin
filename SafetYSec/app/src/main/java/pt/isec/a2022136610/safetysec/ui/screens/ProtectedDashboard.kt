package pt.isec.a2022136610.safetysec.ui.screens

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.ui.components.CountdownAlert
import pt.isec.a2022136610.safetysec.utils.FallDetector
import pt.isec.a2022136610.safetysec.utils.VideoRecordingManager
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@Composable
fun ProtectedDashboard(userName: String, viewModel: AuthViewModel = viewModel()) {
    val generatedCode by viewModel.generatedCode.collectAsState()
    val showCountdown by viewModel.showCountdown.collectAsState()

    var showCodeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- CÂMARA & VÍDEO ---
    val videoManager = remember { VideoRecordingManager(context) }
    // Precisamos de uma View real para o CameraX (mesmo que pequena)
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // --- SENSOR DE QUEDAS ---
    val fallDetector = remember { FallDetector(context) }
    DisposableEffect(Unit) {
        fallDetector.startListening {
            viewModel.triggerCountdown("Queda Detetada (Impacto)")
        }
        onDispose { fallDetector.stopListening() }
    }

    LaunchedEffect(generatedCode) { if (generatedCode != null) showCodeDialog = true }

    // Inicializar Câmara quando a view estiver pronta
    LaunchedEffect(previewView) {
        if (previewView != null) {
            videoManager.startCamera(lifecycleOwner, previewView!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // PREVIEW DA CÂMARA (Pequeno no canto superior direito)
        Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).padding(8.dp)) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView = it }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- CONTEÚDO PRINCIPAL ---
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Modo Protegido", style = MaterialTheme.typography.labelLarge)
                Text("Olá, $userName", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Badge(containerColor = Color.Green) {
                    Text(" Proteção Ativa ", color = Color.Black, modifier = Modifier.padding(4.dp))
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
                    Text("SOS", style = MaterialTheme.typography.headlineLarge)
                }
            }

            Text("Pressione em emergência\n(A gravar 30s após alerta)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            OutlinedButton(onClick = { viewModel.generateAssociationCode() }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text("Associar Monitor")
            }
        }

        if (showCodeDialog && generatedCode != null) {
            AlertDialog(
                onDismissRequest = { showCodeDialog = false },
                title = { Text("Código de Associação") },
                text = { Text(generatedCode!!, style = MaterialTheme.typography.displayMedium) },
                confirmButton = { TextButton(onClick = { showCodeDialog = false }) { Text("Fechar") } }
            )
        }

        if (showCountdown) {
            CountdownAlert(
                reason = "Alerta em curso...",
                onCancel = { pin -> viewModel.verifyPinAndCancel(pin) },
                onTimeout = {
                    // 1. Envia o Alerta Imediatamente
                    viewModel.executeFinalAlert()
                    Toast.makeText(context, "ALERTA ENVIADO! A GRAVAR VÍDEO...", Toast.LENGTH_LONG).show()

                    // 2. Começa a gravar 30s
                    videoManager.startRecording30Seconds { videoUrl ->
                        // 3. Quando acabar upload, atualiza o alerta
                        viewModel.executeFinalAlert(videoUrl)
                        Toast.makeText(context, "Vídeo enviado ao Monitor!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}