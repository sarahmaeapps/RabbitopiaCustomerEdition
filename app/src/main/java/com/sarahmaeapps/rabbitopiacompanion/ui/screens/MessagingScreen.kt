package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sarahmaeapps.rabbitopiacompanion.data.model.Message
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.MessagingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    onBackClick: () -> Unit,
    viewModel: MessagingViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val currentUserEmail = viewModel.getCurrentUserEmail() ?: ""
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Messaging") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    items(messages) { message ->
                        val isCurrentUser = message.senderId.lowercase() == currentUserEmail.lowercase()
                        MessageBubble(message = message, isCurrentUser = isCurrentUser)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Column {
                        if (selectedImageUri != null) {
                            Box(modifier = Modifier.padding(8.dp).size(100.dp)) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { launcher.launch("image/*") }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach Image", tint = MaterialTheme.colorScheme.primary)
                            }

                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (text.isNotBlank() || selectedImageUri != null) {
                                        viewModel.sendMessage(text, selectedImageUri)
                                        text = ""
                                        selectedImageUri = null
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Message Image",
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (message.text.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                }
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
