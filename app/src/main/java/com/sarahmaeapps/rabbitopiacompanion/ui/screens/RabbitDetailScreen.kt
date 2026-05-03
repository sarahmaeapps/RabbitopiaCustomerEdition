package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sarahmaeapps.rabbitopiacompanion.data.model.Pedigree
import com.sarahmaeapps.rabbitopiacompanion.data.model.Rabbit
import com.sarahmaeapps.rabbitopiacompanion.data.model.RabbitRelative
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.RabbitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabbitDetailScreen(
    rabbitId: String,
    onBackClick: () -> Unit,
    onWeighInsClick: () -> Unit,
    onMedicalClick: () -> Unit,
    onContactClick: () -> Unit,
    onEvaluationHistoryClick: (String) -> Unit,
    viewModel: RabbitViewModel,
    isFromMyRabbits: Boolean = false
) {
    val rabbitState by viewModel.selectedRabbit.collectAsState()
    val rabbit = rabbitState
    
    LaunchedEffect(rabbitId) {
        viewModel.selectRabbit(rabbitId)
    }
    
    // Editable states for local data
    var editedGrade by remember(rabbit) { mutableStateOf(rabbit?.grade ?: "") }
    var editedScore by remember(rabbit) { mutableStateOf(rabbit?.arbaScore ?: "") }
    var editedSopScore by remember(rabbit) { mutableStateOf(rabbit?.sopScore ?: "") }
    var editedNotes by remember(rabbit) { mutableStateOf(rabbit?.notes ?: "") }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text(rabbit?.name ?: "Rabbit Details") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isFromMyRabbits && rabbit != null) {
                            TextButton(onClick = {
                                viewModel.updateRabbit(rabbit.copy(
                                    grade = editedGrade,
                                    arbaScore = editedScore,
                                    sopScore = editedSopScore,
                                    notes = editedNotes
                                ))
                            }) {
                                Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (rabbit == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Rabbit not found")
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Section: Name and Image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rabbit.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isFromMyRabbits) {
                                    EditableInfoBox(label = "Grade", value = editedGrade, onValueChange = { editedGrade = it }, subValue = "A - F")
                                    EditableInfoBox(label = "Score", value = editedScore, onValueChange = { editedScore = it }, subValue = "0 - 10")
                                } else {
                                    InfoBox(label = "SOP Score", value = rabbit.sopScore.ifEmpty { "0/100" }, subValue = "Total")
                                }
                            }
                            
                            if (!isFromMyRabbits) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onEvaluationHistoryClick(rabbit.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Evaluation History", fontSize = 12.sp)
                                }
                            }
                        }

                        AsyncImage(
                            model = rabbit.pictureUrl,
                            contentDescription = "Rabbit Picture",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(3.dp, Color.Black, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Middle Section: Details
                    DetailRow("Ear Tattoo #", rabbit.earTattooId)
                    DetailRow("Breed", rabbit.breed)
                    DetailRow("Color Description", rabbit.color)
                    DetailRow("Sex", rabbit.sex)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hutch ID", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = Color.White)
                        HutchBox(rabbit.hutchId)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailRow("Status", rabbit.status)
                        GenerationBox(rabbit.generation)
                    }

                    // Notes and Interactive Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isFromMyRabbits) {
                            EditableNoteBox("Notes", editedNotes, onValueChange = { editedNotes = it }, modifier = Modifier.weight(1f))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onWeighInsClick,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Text("Weights", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onMedicalClick,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Text("Medical", fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Interactive area",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            NoteBox("Notes", rabbit.notes, modifier = Modifier.weight(1f))
                            NoteBox("Interactive area", "Weight & Medical", modifier = Modifier.weight(1f))
                        }
                    }

                    Text(
                        text = "Rabbitopia!",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Red,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Bottom Buttons (Only for For Sale / Wishlist)
                    if (!isFromMyRabbits) {
                        var isAddedToWishList by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { 
                                    viewModel.addToWishList(rabbit) 
                                    isAddedToWishList = true
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddedToWishList) Color.Green.copy(alpha = 0.5f) else Color.White, 
                                    contentColor = Color.Black
                                ),
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Text(if (isAddedToWishList) "Interested!" else "Interested", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onContactClick,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Text("Contact Us", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f))

                    Text(
                        text = "Pedigree Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    PedigreeTree(rabbit.name, rabbit.pedigree)
                }
            }
        }
    }
}

@Composable
fun EditableInfoBox(label: String, value: String, onValueChange: (String) -> Unit, subValue: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, 
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .size(70.dp, 60.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                .wrapContentHeight(Alignment.CenterVertically)
        )
        Text(text = label, fontSize = 12.sp, color = Color.White)
        Text(text = subValue, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun EditableNoteBox(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(8.dp)
        )
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun InfoBox(label: String, value: String, subValue: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp, 60.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Text(text = label, fontSize = 12.sp, color = Color.White)
        Text(text = subValue, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun HutchBox(hutchId: String) {
    val parts = hutchId.split(" ")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { part ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = part, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GenerationBox(gen: Int) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "G$gen", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(text = "Generation", fontSize = 10.sp)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.White)
        Text(
            text = value.ifEmpty { "---" },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.5f))
                .padding(4.dp),
            color = Color.White
        )
    }
}

@Composable
fun NoteBox(label: String, content: String, modifier: Modifier = Modifier, isInteractive: Boolean = false, onClick: () -> Unit = {}) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .then(if (isInteractive) Modifier.clickable { onClick() } else Modifier)
                .padding(8.dp)
        ) {
            Text(text = content, fontSize = 14.sp, color = Color.Black)
        }
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PedigreeTree(rabbitName: String, pedigree: Pedigree?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = rabbitName, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                
                Column {
                    RelativeRow(label = "Father", relative = pedigree?.father)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    RelativeRow(label = "Mother", relative = pedigree?.mother)
                }
            }
        }
    }
}

@Composable
fun RelativeRow(label: String, relative: RabbitRelative?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.width(60.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = relative?.name ?: "Unknown", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Column {
            Text(text = "GF: ${relative?.father?.name ?: "Unknown"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "GM: ${relative?.mother?.name ?: "Unknown"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
