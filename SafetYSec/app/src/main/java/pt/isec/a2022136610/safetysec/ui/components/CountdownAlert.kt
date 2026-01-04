package pt.isec.a2022136610.safetysec.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import pt.isec.a2022136610.safetysec.R

@Composable
fun CountdownAlert(
    reason: String,
    durationSeconds: Int = 10,
    onCancel: (String) -> Unit,
    onTimeout: () -> Unit
) {
    var timeLeft by remember { mutableFloatStateOf(durationSeconds.toFloat()) }
    var pinInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Countdown Logic
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(100L) // Update every 100ms for smooth animation
            timeLeft -= 0.1f
        }
        onTimeout()
    }

    // Dialog that fills the screen but allows seeing behind it slightly (optional)
    Dialog(
        onDismissRequest = { /* Prevent dismissal by clicking outside */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = maxWidth > maxHeight
            val progress = timeLeft / durationSeconds

            // --- REUSABLE COMPONENTS ---
            val TimerDisplay = @Composable { modifier: Modifier ->
                Box(contentAlignment = Alignment.Center, modifier = modifier) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        // Background Circle
                        drawArc(
                            color = Color.DarkGray,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress Circle
                        drawArc(
                            color = Color.Red,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.0f", timeLeft),
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SEC",
                            color = Color.Red,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val ControlPanel = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = reason.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter PIN to Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinInput = it
                                if (it.length == 4) {
                                    onCancel(it)
                                    pinInput = "" // Clear on attempt
                                }
                            }
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onTimeout() }, // Immediate Trigger
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SEND ALERT NOW")
                    }
                }
            }

            // --- LAYOUT SWITCHING ---
            if (isLandscape) {
                // LANDSCAPE: Row (Timer Left | Controls Right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerDisplay(Modifier.weight(1f))
                    // Wrap controls in scroll in case keyboard hides buttons
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        ControlPanel(Modifier.fillMaxWidth())
                    }
                }
            } else {
                // PORTRAIT: Column (Timer Top | Controls Bottom)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    TimerDisplay(Modifier.weight(1f))
                    ControlPanel(Modifier.fillMaxWidth())
                }
            }
        }
    }
}