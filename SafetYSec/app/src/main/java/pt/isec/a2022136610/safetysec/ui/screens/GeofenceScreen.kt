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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.R
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
    var radiusInput by remember { mutableStateOf("100") }

    LaunchedEffect(userId) { viewModel.loadTargetUser(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_geofence)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

                    Text(stringResource(R.string.protected_user, targetUser!!.name), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(32.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.set_safe_zone), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.geofence_instruction))

                            Spacer(Modifier.height(8.dp))

                            if (targetUser!!.lastLocation != null) {
                                Text(
                                    stringResource(R.string.current_center, targetUser!!.lastLocation!!.latitude, targetUser!!.lastLocation!!.longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(stringResource(R.string.error_location), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = radiusInput,
                        onValueChange = { radiusInput = it },
                        label = { Text(stringResource(R.string.radius_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val radius = radiusInput.toDoubleOrNull()
                            if (radius != null && targetUser!!.lastLocation != null) {
                                viewModel.createGeofenceRule(
                                    protectedId = userId,
                                    center = targetUser!!.lastLocation!!,
                                    radius = radius
                                )
                                Toast.makeText(context, context.getString(R.string.success_fence), Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = targetUser!!.lastLocation != null
                    ) {
                        Text(stringResource(R.string.btn_activate_fence))
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}