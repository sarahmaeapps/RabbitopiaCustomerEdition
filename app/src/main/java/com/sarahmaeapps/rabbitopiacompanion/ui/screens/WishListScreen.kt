package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sarahmaeapps.rabbitopiacompanion.data.model.Rabbit
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.RabbitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    onBackClick: () -> Unit,
    onRabbitClick: (String) -> Unit,
    viewModel: RabbitViewModel
) {
    val wishListRabbits by viewModel.wishListRabbits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWishList()
    }

    RabbitBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("My Wish List") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (wishListRabbits.isEmpty()) {
                    Text(
                        text = "Your wish list is empty.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(wishListRabbits) { rabbit ->
                            ForSaleRabbitItem(rabbit = rabbit, onClick = { onRabbitClick(rabbit.id) })
                        }
                    }
                }
            }
        }
    }
}
