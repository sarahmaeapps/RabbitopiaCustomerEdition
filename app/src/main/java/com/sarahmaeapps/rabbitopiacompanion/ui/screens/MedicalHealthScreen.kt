package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sarahmaeapps.rabbitopiacompanion.data.local.MedicalCareEntry
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.LocalViewModel
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.RabbitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalHealthScreen(
    onBackClick: () -> Unit,
    localViewModel: LocalViewModel,
    rabbitViewModel: RabbitViewModel
) {
    val medicalEntries by localViewModel.allMedicalCare.collectAsState()
    val rabbits by rabbitViewModel.rabbits.collectAsState()
    
    var selectedRabbitIdFilter by remember { mutableStateOf<String?>(null) }
    var filterExpanded by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<MedicalCareEntry?>(null) }

    val filteredEntries = if (selectedRabbitIdFilter == null) {
        medicalEntries
    } else {
        medicalEntries.filter { it.rabbitId == selectedRabbitIdFilter }
    }

    val totalExpenses = filteredEntries.sumOf { it.cost }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Medical & Health") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { filterExpanded = true }) {
                                val currentFilterName = if (selectedRabbitIdFilter == null) "All" else rabbits.find { it.id == selectedRabbitIdFilter }?.name ?: "All"
                                Text(currentFilterName, color = Color.White)
                            }
                            DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                                DropdownMenuItem(text = { Text("All Rabbits") }, onClick = { selectedRabbitIdFilter = null; filterExpanded = false })
                                rabbits.forEach { rabbit ->
                                    DropdownMenuItem(text = { Text(rabbit.name) }, onClick = { selectedRabbitIdFilter = rabbit.id; filterExpanded = false })
                                }
                            }
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
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Total Medical Expenses", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(text = "$${String.format("%.2f", totalExpenses)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Text("No medical entries yet.", modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEntries) { entry ->
                            val rabbitName = rabbits.find { it.id == entry.rabbitId }?.name ?: "Unknown Rabbit"
                            MedicalItem(entry, rabbitName, onClick = { selectedEntry = entry })
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddMedicalDialog(
                    rabbits = rabbits.map { it.id to it.name },
                    onDismiss = { showAddDialog = false },
                    onAdd = { localViewModel.insertMedicalCare(it) }
                )
            }

            if (selectedEntry != null) {
                val rabbitName = rabbits.find { it.id == selectedEntry!!.rabbitId }?.name ?: "Unknown Rabbit"
                MedicalDetailDialog(entry = selectedEntry!!, rabbitName = rabbitName, onDismiss = { selectedEntry = null })
            }
        }
    }
}

@Composable
fun MedicalItem(entry: MedicalCareEntry, rabbitName: String, onClick: () -> Unit) {
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
                Text(text = rabbitName, style = MaterialTheme.typography.titleMedium)
                Text(text = entry.treatment, style = MaterialTheme.typography.bodyMedium)
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "$${entry.cost}", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalDialog(rabbits: List<Pair<String, String>>, onDismiss: () -> Unit, onAdd: (MedicalCareEntry) -> Unit) {
    var selectedRabbitId by remember { mutableStateOf(rabbits.firstOrNull()?.first ?: "") }
    var treatment by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medical Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rabbit Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedName = rabbits.find { it.first == selectedRabbitId }?.second ?: "Select Rabbit"
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rabbit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        rabbits.forEach { rabbit ->
                            DropdownMenuItem(
                                text = { Text(rabbit.second) },
                                onClick = {
                                    selectedRabbitId = rabbit.first
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Treatment") })
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") })
                OutlinedTextField(value = medications, onValueChange = { medications = it }, label = { Text("Medications") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Vet Notes") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(MedicalCareEntry(
                    rabbitId = selectedRabbitId,
                    treatment = treatment,
                    cost = cost.toDoubleOrNull() ?: 0.0,
                    medications = medications,
                    vetNotes = notes,
                    date = System.currentTimeMillis()
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
fun MedicalDetailDialog(entry: MedicalCareEntry, rabbitName: String, onDismiss: () -> Unit) {
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Medical Record: $rabbitName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Date: $dateStr")
                Text("Treatment: ${entry.treatment}")
                Text("Cost: $${entry.cost}")
                Text("Medications: ${entry.medications}")
                Text("Vet Notes: ${entry.vetNotes}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
