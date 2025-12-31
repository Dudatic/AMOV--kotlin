package pt.isec.a2022136610.safetysec.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsScreen(
    alertId: String,
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val alert by viewModel.selectedAlert.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(alertId) {
        viewModel.getAlert(alertId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (alert != null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert!!.status == "ACTIVE") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = alert!!.ruleType.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (alert!!.status == "ACTIVE") Color.Red else Color(0xFF2E7D32)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Status: ${alert!!.status}", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        val date = alert!!.timestamp.toDate()
                        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        Text("Time: ${format.format(date)}")

                        if (alert!!.cancelReason != null) {
                            Text("Details: ${alert!!.cancelReason}")
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Location
                Text("Location", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                if (alert!!.location != null) {
                    val lat = alert!!.location!!.latitude
                    val lng = alert!!.location!!.longitude

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$lat, $lng")
                        Button(onClick = {
                            val label = Uri.encode("Emergency Location")
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Maps")
                        }
                    }
                } else {
                    Text("No location data available.", color = Color.Gray)
                }

                Spacer(Modifier.height(24.dp))

                // Video
                Text("Emergency Video", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))

                if (alert!!.videoUrl != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setMediaController(MediaController(ctx))
                                    setVideoURI(Uri.parse(alert!!.videoUrl))
                                    setOnPreparedListener {
                                        // Auto-play or just ready
                                        it.setVolume(1f, 1f)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (alert!!.status == "ACTIVE") {
                            Text("Waiting for video upload...")
                        } else {
                            Text("No video recorded.")
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Actions
                if (alert!!.status == "ACTIVE") {
                    Button(
                        onClick = { viewModel.resolveAlert(alert!!.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("MARK AS RESOLVED")
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