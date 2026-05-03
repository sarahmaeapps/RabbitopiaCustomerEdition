package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sarahmaeapps.rabbitopiacompanion.data.local.HousingEntity
import com.sarahmaeapps.rabbitopiacompanion.data.local.RepairEntry
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.LocalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingDetailScreen(
    hutchId: String,
    onBackClick: () -> Unit,
    viewModel: LocalViewModel
) {
    val housingList by viewModel.allHousing.collectAsState()
    val housing = housingList.find { it.hutchId == hutchId } ?: HousingEntity(hutchId = hutchId)

    var condition by remember(housing) { mutableStateOf(housing.condition) }
    var signsOfPredators by remember(housing) { mutableStateOf(housing.signsOfPredators) }
    var upgrades by remember(housing) { mutableStateOf(housing.upgrades) }
    var showRepairDialog by remember { mutableStateOf(false) }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Hutch Detail") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            viewModel.insertHousing(housing.copy(
                                condition = condition,
                                signsOfPredators = signsOfPredators,
                                upgrades = upgrades
                            ))
                        }) {
                            Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hutch ID Display (Spinner style as requested)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hutch ID: ", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    HousingHutchBox(hutchId)
                }

                Text(text = "Current Residents", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (housing.residents.isEmpty()) {
                            Text("No residents listed.", color = Color.White.copy(alpha = 0.7f))
                        } else {
                            housing.residents.forEach { resident ->
                                Text(text = "• $resident", color = Color.White)
                            }
                        }
                    }
                }

                // Condition Dropdown
                ConditionDropdown(selected = condition, onSelected = { condition = it })

                // Predators Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = signsOfPredators,
                        onCheckedChange = { signsOfPredators = it },
                        colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
                    )
                    Text(text = "Signs of Predators", color = Color.White)
                }

                // Upgrades Text Entry
                OutlinedTextField(
                    value = upgrades,
                    onValueChange = { upgrades = it },
                    label = { Text("Upgrades and Improvements") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                // Repair and Maintenance History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Repair and Maintenance History", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    IconButton(onClick = { showRepairDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Repair", tint = Color.White)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    housing.repairHistory.forEach { repair ->
                        RepairItem(repair)
                    }
                }
            }

            if (showRepairDialog) {
                AddRepairDialog(
                    onDismiss = { showRepairDialog = false },
                    onAdd = { task ->
                        val newList = housing.repairHistory.toMutableList().apply {
                            add(RepairEntry(date = System.currentTimeMillis(), task = task))
                        }
                        viewModel.insertHousing(housing.copy(repairHistory = newList))
                        showRepairDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun HousingHutchBox(hutchId: String) {
    val parts = hutchId.map { it.toString() }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { part ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = part, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun RepairItem(repair: RepairEntry) {
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(repair.date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(text = repair.task, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionDropdown(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("Excellent", "Good", "Average", "Fair", "Poor")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Condition") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddRepairDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var task by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Repair/Maintenance") },
        text = {
            OutlinedTextField(value = task, onValueChange = { task = it }, label = { Text("Task/Description") })
        },
        confirmButton = {
            Button(onClick = { if (task.isNotBlank()) onAdd(task) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
