package com.example.gwt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gwt.R
import com.example.gwt.model.BlackspotReport
import com.example.gwt.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = viewModel()
) {
    val reports by viewModel.blackspots.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_reports), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdminStatsSummary(reports)
                }
                items(reports) { report ->
                    BlackspotAdminCard(report) { status, comment ->
                        viewModel.updateReportStatus(report.id, status, comment)
                    }
                }
            }
        }

        if (isUpdating) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun AdminStatsSummary(reports: List<BlackspotReport>) {
    val pending = reports.count { it.status == "Pending" }
    val cleaning = reports.count { it.status == "In Progress" }
    val cleaned = reports.count { it.status == "Cleaned" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(stringResource(R.string.pending), pending, Color(0xFFE65100), Modifier.weight(1f))
        StatCard(stringResource(R.string.cleaning), cleaning, Color(0xFF1565C0), Modifier.weight(1f))
        StatCard(stringResource(R.string.cleaned), cleaned, Color(0xFF2E7D32), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        }
    }
}

@Composable
fun BlackspotAdminCard(
    report: BlackspotReport,
    onRespond: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf(report.adminComment) }
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = report.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(report.status)
                    Text(
                        text = sdf.format(Date(report.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Location: ${"%.4f".format(report.latitude)}, ${"%.4f".format(report.longitude)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (report.description.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.description)}: ${report.description}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (report.adminComment.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stringResource(R.string.admin_comment)}: ${report.adminComment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.take_action))
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.update_status)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.admin_comment)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.select_status))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatusButton(stringResource(R.string.pending), Color.Gray, report.status == "Pending") { onRespond("Pending", comment); showDialog = false }
                        StatusButton(stringResource(R.string.cleaning), Color.Blue, report.status == "In Progress") { onRespond("In Progress", comment); showDialog = false }
                        StatusButton(stringResource(R.string.cleaned), Color.Green, report.status == "Cleaned") { onRespond("Cleaned", comment); showDialog = false }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val statusText = when (status) {
        "Cleaned" -> stringResource(R.string.cleaned)
        "In Progress" -> stringResource(R.string.cleaning)
        else -> stringResource(R.string.pending)
    }
    val color = when (status) {
        "Cleaned" -> Color(0xFF2E7D32)
        "In Progress" -> Color(0xFF1565C0)
        else -> Color(0xFFE65100)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusButton(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.1f),
            contentColor = if (isSelected) Color.White else color
        ),
        modifier = Modifier.padding(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
