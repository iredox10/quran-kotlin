package com.nur.quran.ui.components.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

enum class AuthFormMode {
    LOGIN, REGISTER, FORGOT
}

/**
 * Cloud Sync Card matching web app Profile.jsx lines 336-424.
 */
@Composable
fun ProfileCloudSyncCard(
    signedIn: Boolean,
    busy: Boolean,
    error: String?,
    message: String?,
    lastSyncAt: Long?,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    isSyncSuccess: Boolean,
    isSyncError: Boolean,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String) -> Unit,
    onForgot: (email: String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthForm by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf(AuthFormMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "CLOUD SYNC")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark)
        ) {
            if (!signedIn) {
                // Non-logged-in Expandable Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuthForm = !showAuthForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(hGoldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.CloudUpload,
                                    contentDescription = null,
                                    tint = hGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Sign in to sync",
                                    fontFamily = fontFamilyBody,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                                Text(
                                    text = "Backup bookmarks & settings",
                                    fontFamily = fontFamilyBody,
                                    fontSize = 12.sp,
                                    color = hInkMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = NurIcons.ChevronDown,
                            contentDescription = "Expand",
                            tint = hInkMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (showAuthForm) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(
                        visible = showAuthForm,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)

                            // Error Alert
                            if (!error.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1AEF4444))
                                        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            // Success Alert
                            if (!message.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1A10B981))
                                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            // Optional Name Field for Registration
                            if (authMode == AuthFormMode.REGISTER) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("Your Name", fontSize = 13.sp, color = hInkMuted) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = hWhite,
                                        unfocusedContainerColor = hWhite,
                                        focusedBorderColor = hGold,
                                        unfocusedBorderColor = hBoneDark
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    onClearMessage()
                                },
                                placeholder = { Text("Email", fontSize = 13.sp, color = hInkMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = hWhite,
                                    unfocusedContainerColor = hWhite,
                                    focusedBorderColor = hGold,
                                    unfocusedBorderColor = hBoneDark
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Password Field
                            if (authMode != AuthFormMode.FORGOT) {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        onClearMessage()
                                    },
                                    placeholder = { Text("Password", fontSize = 13.sp, color = hInkMuted) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = hWhite,
                                        unfocusedContainerColor = hWhite,
                                        focusedBorderColor = hGold,
                                        unfocusedBorderColor = hBoneDark
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    when (authMode) {
                                        AuthFormMode.LOGIN -> onLogin(email.trim(), password)
                                        AuthFormMode.REGISTER -> onRegister(email.trim(), password)
                                        AuthFormMode.FORGOT -> onForgot(email.trim())
                                    }
                                },
                                enabled = !busy && email.isNotBlank() && (authMode == AuthFormMode.FORGOT || password.isNotBlank()),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = hInk,
                                    contentColor = hWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = hGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = when (authMode) {
                                            AuthFormMode.LOGIN -> "Sign In"
                                            AuthFormMode.REGISTER -> "Create Account"
                                            AuthFormMode.FORGOT -> "Send Recovery Link"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Switchers
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        onClearMessage()
                                        authMode = if (authMode == AuthFormMode.LOGIN) AuthFormMode.REGISTER else AuthFormMode.LOGIN
                                    }
                                ) {
                                    Text(
                                        text = if (authMode == AuthFormMode.LOGIN) "Don't have an account? Sign up" else "Already have an account? Sign in",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = hGold
                                    )
                                }

                                if (authMode != AuthFormMode.FORGOT) {
                                    Text(
                                        text = "Forgot Password?",
                                        fontSize = 11.sp,
                                        color = hInkMuted,
                                        modifier = Modifier.clickable {
                                            onClearMessage()
                                            authMode = AuthFormMode.FORGOT
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Logged-in Backup & Restore Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(hGoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.CloudUpload,
                                contentDescription = null,
                                tint = hGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Backup & Restore",
                                fontFamily = fontFamilyUi,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = "Last synced: ${ProfileUtils.timeAgo(lastSyncAt)}",
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                color = hInkMuted
                            )
                        }
                    }

                    // Live Status Row
                    if (isSyncing || isSyncSuccess || isSyncError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = hGold,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = syncStatusMessage ?: "Syncing data...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hGold
                                )
                            } else if (isSyncSuccess) {
                                Text(
                                    text = "✓ Sync complete successfully.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            } else if (isSyncError) {
                                Text(
                                    text = "Failed to sync. Please try again.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    // Restore & Backup Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Restore Button
                        OutlinedButton(
                            onClick = onRestore,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, hBoneDark),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = hWhite,
                                contentColor = hInk
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = NurIcons.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Backup Button
                        Button(
                            onClick = onBackup,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = hInk,
                                contentColor = hWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = NurIcons.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Backup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
