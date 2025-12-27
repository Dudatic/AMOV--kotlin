package pt.isec.a2022136610.safetysec.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// Lista de permissões obrigatórias
fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // Android 10+ (API 29) precisa de Activity Recognition para detetar movimento
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    // Android 13+ (API 33) precisa de permissão para Notificações
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    return permissions.toTypedArray()
}

// Verifica se tem todas as permissões e IMPRIME no Logcat qual falta
fun hasAllPermissions(context: Context): Boolean {
    val required = getRequiredPermissions()
    var allGranted = true

    for (permission in required) {
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            Log.d("SAFETYSEC_DEBUG", "Falta a permissão: $permission")
            allGranted = false
        }
    }
    return allGranted
}

// Abre as definições da App no Android
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

// Composable que gere o pedido inicial
@Composable
fun PermissionHandler(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(hasAllPermissions(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(getRequiredPermissions())
        } else {
            onPermissionsGranted()
        }
    }
}