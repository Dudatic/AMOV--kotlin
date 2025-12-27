package pt.isec.a2022136610.safetysec.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    userId: String,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    // Agora observamos o 'targetUser' (específico para este ecrã)
    val targetUser by viewModel.targetUser.collectAsState()

    // Assim que o ecrã abre, mandamos buscar os dados deste ID específico
    LaunchedEffect(userId) {
        viewModel.loadTargetUser(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Localização") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (targetUser != null) {
                // Se já carregámos os dados, mostra o ecrã
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        targetUser!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        targetUser!!.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(Modifier.height(32.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Última Posição:", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))

                            if (targetUser!!.lastLocation != null) {
                                Text("Lat: ${targetUser!!.lastLocation!!.latitude}")
                                Text("Long: ${targetUser!!.lastLocation!!.longitude}")
                            } else {
                                Text("A aguardar sinal de GPS...", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Botão para abrir Google Maps Externo
                    Button(
                        onClick = {
                            targetUser!!.lastLocation?.let { loc ->
                                val label = Uri.encode(targetUser!!.name)
                                val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}($label)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                        },
                        enabled = targetUser!!.lastLocation != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver no Google Maps App")
                    }
                }
            } else {
                // Loading enquanto vai ao Firebase
                CircularProgressIndicator()
            }
        }
    }
}