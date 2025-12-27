package pt.isec.a2022136610.safetysec.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountdownAlert(
    reason: String,
    onCancel: (String) -> Unit, // Recebe o PIN introduzido
    onTimeout: () -> Unit // O que fazer quando o tempo acaba
) {
    var timeLeft by remember { mutableIntStateOf(10) } // 10 Segundos
    var pinInput by remember { mutableStateOf("") }

    // Lógica do Temporizador
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        // Quando chega a 0, dispara o Timeout (Envia o Alerta)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "ALERTA EM CURSO!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Motivo: $reason",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            // O Contador Gigante
            Text(
                "$timeLeft",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Text("segundos para cancelar", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            // Input do PIN
            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    if (it.length <= 4) pinInput = it
                },
                label = { Text("PIN de Cancelamento") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onCancel(pinInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CANCELAR ALERTA")
            }
        }
    }
}