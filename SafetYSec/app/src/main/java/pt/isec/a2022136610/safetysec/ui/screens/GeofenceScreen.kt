package pt.isec.a2022136610.safetysec.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceScreen(
    navController: NavController,
    userId: String,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val targetUser by viewModel.targetUser.collectAsState()

    // Raio da cerca (em metros) - Default 100m
    var radiusInput by remember { mutableStateOf("100") }

    // Carrega o utilizador alvo ao abrir o ecrã
    LaunchedEffect(userId) {
        viewModel.loadTargetUser(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Cerca Virtual") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp)) {

            if (targetUser != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("Protegido: ${targetUser!!.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(32.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Definir Zona Segura", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("A cerca será criada usando a ÚLTIMA LOCALIZAÇÃO conhecida deste utilizador como centro.")

                            Spacer(Modifier.height(8.dp))

                            if (targetUser!!.lastLocation != null) {
                                Text(
                                    "Centro Atual: ${targetUser!!.lastLocation!!.latitude}, ${targetUser!!.lastLocation!!.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text("Aviso: Localização desconhecida. Não é possível criar cerca.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = radiusInput,
                        onValueChange = { radiusInput = it },
                        label = { Text("Raio (metros)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val radius = radiusInput.toDoubleOrNull()
                            if (radius != null && targetUser!!.lastLocation != null) {
                                // Chama a função para criar a regra no ViewModel
                                viewModel.createGeofenceRule(
                                    protectedId = userId,
                                    center = targetUser!!.lastLocation!!,
                                    radius = radius
                                )
                                Toast.makeText(context, "Cerca ativada com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Erro: Verifique o raio e se existe localização.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = targetUser!!.lastLocation != null
                    ) {
                        Text("ATIVAR CERCA")
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}