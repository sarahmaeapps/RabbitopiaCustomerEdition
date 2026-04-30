package com.sarahmaeapps.rabbitopiacompanion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.sarahmaeapps.rabbitopiacompanion.ui.components.RabbitBackground
import com.sarahmaeapps.rabbitopiacompanion.ui.theme.RabbitopiaLogoColor

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()

    RabbitBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Login",
                        style = MaterialTheme.typography.headlineLarge,
                        color = RabbitopiaLogoColor,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        }
                    )

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                val normalizedEmail = email.lowercase().trim()
                                if (isSignUp) {
                                    auth.createUserWithEmailAndPassword(normalizedEmail, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val profileUpdates = UserProfileChangeRequest.Builder()
                                                    .setDisplayName(name)
                                                    .build()
                                                auth.currentUser?.updateProfile(profileUpdates)
                                                    ?.addOnCompleteListener {
                                                        isLoading = false
                                                        onLoginSuccess()
                                                    }
                                            } else {
                                                isLoading = false
                                                errorMessage = task.exception?.message
                                            }
                                        }
                                } else {
                                    auth.signInWithEmailAndPassword(normalizedEmail, password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = task.exception?.message
                                            }
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isSignUp) "Sign Up" else "Login")
                        }

                        TextButton(onClick = { isSignUp = !isSignUp }) {
                            Text(if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up")
                        }
                    }
                }
            }
        }
    }
}
