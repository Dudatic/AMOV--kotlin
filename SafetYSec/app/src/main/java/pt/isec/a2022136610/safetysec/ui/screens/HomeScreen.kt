package pt.isec.a2022136610.safetysec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.UserRole
import pt.isec.a2022136610.safetysec.utils.LocationHelper
import pt.isec.a2022136610.safetysec.utils.PermissionHandler
import pt.isec.a2022136610.safetysec.utils.hasAllPermissions
import pt.isec.a2022136610.safetysec.utils.openAppSettings
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var isMonitorMode by remember { mutableStateOf(true) }
    var permissionsGranted by remember { mutableStateOf(hasAllPermissions(context)) }
    val locationHelper = remember { LocationHelper(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsGranted = hasAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationHelper.stopLocationUpdates()
        }
    }

    LaunchedEffect(Unit) { viewModel.loadCurrentUser() }

    LaunchedEffect(permissionsGranted, currentUser) {
        if (permissionsGranted && (currentUser?.role == UserRole.PROTECTED || currentUser?.role == UserRole.BOTH)) {
            locationHelper.startLocationUpdates { location ->
                viewModel.updateUserLocation(location)
            }
        }
    }

    PermissionHandler { permissionsGranted = true }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (currentUser?.role == UserRole.BOTH) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Switch(
                                checked = isMonitorMode,
                                onCheckedChange = { isMonitorMode = it },
                                modifier = Modifier.scale(0.8f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isMonitorMode) "M" else "P",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        locationHelper.stopLocationUpdates()
                        viewModel.signOut()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (!permissionsGranted) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.perm_desc), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.perm_list), textAlign = TextAlign.Start, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.perm_btn_settings))
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { permissionsGranted = hasAllPermissions(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.perm_btn_refresh))
                    }
                }
            } else {
                val effectiveRole = if (currentUser?.role == UserRole.BOTH) {
                    if (isMonitorMode) UserRole.MONITOR else UserRole.PROTECTED
                } else {
                    currentUser?.role
                }

                when (effectiveRole) {
                    UserRole.MONITOR -> {
                        MonitorDashboard(
                            userName = currentUser?.name ?: stringResource(R.string.def_user_monitor),
                            onUserClick = { userId -> navController.navigate("map/$userId") },
                            onGeofenceClick = { userId -> navController.navigate("geofence/$userId") },
                            onRuleManageClick = { userId -> navController.navigate("rules/$userId") },
                            onAlertClick = { alertId -> navController.navigate("alert/$alertId") }
                        )
                    }
                    UserRole.PROTECTED -> {
                        ProtectedDashboard(
                            userName = currentUser?.name ?: stringResource(R.string.def_user_protected),
                            onHistoryClick = { navController.navigate("history") }
                        )
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}