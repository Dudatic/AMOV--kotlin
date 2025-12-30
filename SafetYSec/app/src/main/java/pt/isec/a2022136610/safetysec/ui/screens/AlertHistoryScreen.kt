package pt.isec.a2022136610.safetysec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AlertHistoryScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchAlertHistory()
    }

    val history by viewModel.alertHistory.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            Text(stringResource(R.string.no_history))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { alert ->
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        ListItem(
                            headlineContent = { Text(alert.ruleType.name) },
                            supportingContent = {
                                val date = alert.timestamp.toDate()
                                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                Text("${format.format(date)} - ${alert.status}")
                            },
                            trailingContent = {
                                if (alert.cancelReason != null) {
                                    Text("Reason: ${alert.cancelReason}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}