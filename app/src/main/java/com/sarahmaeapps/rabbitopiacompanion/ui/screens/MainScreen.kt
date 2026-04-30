package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarahmaeapps.rabbitopiacompanion.R
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.theme.RabbitopiaLogoColor

@Composable
fun MainScreen(
    onMyRabbitsClick: () -> Unit,
    onMyPurchasesClick: () -> Unit,
    onMessagingClick: () -> Unit,
    onForSaleClick: () -> Unit,
    onWishListClick: () -> Unit
) {
    val greetings = remember {
        listOf(
            "Hope your day is hopping with joy!",
            "Every bunny loves you!",
            "You're ear-resistible!",
            "Sending you some bunny love!",
            "Keep calm and carrot on!",
            "Some bunny is thinking of you!",
            "Have a bun-derful day!",
            "Bounce into your day with a smile!",
            "You're doing great, some-bunny is proud of you!"
        )
    }

    val greeting = remember { greetings.random() }
    val newSaigon = FontFamily(Font(R.font.newsaigon))

    RabbitBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Greeting Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "Rabbitopia!",
                fontFamily = newSaigon,
                fontSize = 48.sp,
                textAlign = TextAlign.Center,
                color = RabbitopiaLogoColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            MainButton(text = "My Rabbits", onClick = onMyRabbitsClick)
            MainButton(text = "My Purchases", onClick = onMyPurchasesClick)
            MainButton(text = "Messaging", onClick = onMessagingClick)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onForSaleClick,
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "Animals For Sale",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = onWishListClick,
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(
                        text = "Wish List",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MainButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
