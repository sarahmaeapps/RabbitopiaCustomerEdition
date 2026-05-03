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
import androidx.compose.ui.unit.dp
import com.sarahmaeapps.rabbitopiacompanion.data.local.HousingEntity
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.LocalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingScreen(
    onBackClick: () -> Unit,
    onHousingClick: (String) -> Unit,
    viewModel: LocalViewModel
) {
    val housingList by viewModel.allHousing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Housing") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Hutch")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (housingList.isEmpty()) {
                    Text("No housing entries yet.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(housingList) { housing ->
                            HutchItem(housing, onClick = { onHousingClick(housing.hutchId) })
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddHutchDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { hutchId ->
                        viewModel.insertHousing(HousingEntity(hutchId = hutchId))
                        showAddDialog = false
                        onHousingClick(hutchId)
                    }
                )
            }
        }
    }
}

@Composable
fun HutchItem(housing: HousingEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Hutch ${housing.hutchId}", style = MaterialTheme.typography.titleLarge)
            Text(text = housing.condition, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddHutchDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var letter by remember { mutableStateOf("A") }
    var number by remember { mutableStateOf("1") }
    
    val letters = ('A'..'Z').map { it.toString() }
    val numbers = (0..9).map { it.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Hutch") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Spinner(label = "Letter", options = letters, selected = letter, onSelected = { letter = it })
                Spinner(label = "Number", options = numbers, selected = number, onSelected = { number = it })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd("$letter$number") }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Spinner(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.width(100.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
