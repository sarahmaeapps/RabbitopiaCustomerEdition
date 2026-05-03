package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarahmaeapps.rabbitopiacompanion.data.local.FeedEntry
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.LocalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onBackClick: () -> Unit,
    viewModel: LocalViewModel
) {
    val feedEntries by viewModel.allFeedEntries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<FeedEntry?>(null) }

    val totalCost = feedEntries.sumOf { it.price }
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    val average30Day = feedEntries.filter { it.date >= thirtyDaysAgo }.map { it.price }.average().let { if (it.isNaN()) 0.0 else it }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Feed Tracking") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Entry")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Header Stats
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(label = "Total Cost", value = "$${String.format("%.2f", totalCost)}")
                    StatBox(label = "30-Day Avg", value = "$${String.format("%.2f", average30Day)}")
                }

                if (feedEntries.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Text("No feed entries yet.", modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(feedEntries) { entry ->
                            FeedItem(entry, onClick = { selectedEntry = entry })
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddFeedDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { viewModel.insertFeedEntry(it) }
                )
            }

            if (selectedEntry != null) {
                FeedDetailDialog(entry = selectedEntry!!, onDismiss = { selectedEntry = null })
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun FeedItem(entry: FeedEntry, onClick: () -> Unit) {
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = entry.brand, style = MaterialTheme.typography.titleMedium)
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "$${entry.price}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddFeedDialog(onDismiss: () -> Unit, onAdd: (FeedEntry) -> Unit) {
    var brand by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var lbs by remember { mutableStateOf("") }
    var prot by remember { mutableStateOf("") }
    var fib by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Feed Purchase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") })
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lbs, onValueChange = { lbs = it }, label = { Text("Lbs") }, modifier = Modifier.weight(1f))
                }
                Text(text = "Formulation", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = prot, onValueChange = { prot = it }, label = { Text("Prot %") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fib, onValueChange = { fib = it }, label = { Text("Fib %") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat %") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(FeedEntry(
                    brand = brand,
                    supplier = supplier,
                    price = price.toDoubleOrNull() ?: 0.0,
                    lbs = lbs.toDoubleOrNull() ?: 0.0,
                    prot = prot.toDoubleOrNull() ?: 0.0,
                    fib = fib.toDoubleOrNull() ?: 0.0,
                    fat = fat.toDoubleOrNull() ?: 0.0
                ))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FeedDetailDialog(entry: FeedEntry, onDismiss: () -> Unit) {
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.brand) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Date: $dateStr")
                Text("Supplier: ${entry.supplier}")
                Text("Price: $${entry.price}")
                Text("Weight: ${entry.lbs} lbs")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Formulation:", fontWeight = FontWeight.Bold)
                Text("Protein: ${entry.prot}%")
                Text("Fiber: ${entry.fib}%")
                Text("Fat: ${entry.fat}%")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
