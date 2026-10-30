package com.nur.quran.ui.components.profile



import androidx.compose.ui.res.stringResource

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nur.quran.R
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
    syncError: String?,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String, name: String) -> Unit,
    onForgot: (email: String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    googleAvailable: Boolean = false,
    onContinueWithGoogle: () -> Unit = {}
) {
    var showAuthForm by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf(AuthFormMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val syncStatusActive = isSyncing || isSyncSuccess || syncError != null

    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = stringResource(R.string.ui_cloud_sync_ec7cf7))

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
                                    text = stringResource(R.string.ui_sign_in_to_sync_75dce3),
                                    fontFamily = fontFamilyBody,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                                Text(
                                    text = stringResource(R.string.ui_backup_bookmarks_settings_387bc7),
                                    fontFamily = fontFamilyBody,
                                    fontSize = 12.sp,
                                    color = hInkMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = NurIcons.ChevronDown,
                            contentDescription = stringResource(R.string.ui_expand_9869e5),
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

                            if (googleAvailable && authMode != AuthFormMode.FORGOT) {
                                OutlinedButton(
                                    onClick = onContinueWithGoogle,
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, Color(0xFF747775)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1F1F1F)
                                    )
                                ) {
                                    // Official Google asset. Clip only its surrounding button chrome.
                                    Box(Modifier.size(20.dp).clipToBounds(), contentAlignment = Alignment.Center) {
                                        Image(painterResource(R.drawable.google_signin_icon), null,
                                            modifier = Modifier.requiredSize(40.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.ui_continue_with_google_ccc5b0), fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                                }
                                Text(stringResource(R.string.ui_or_continue_with_email_4ca55f), modifier = Modifier.align(Alignment.CenterHorizontally),
                                    color = hInkMuted, fontSize = 12.sp)
                            }

                            // Optional Name Field for Registration
                            if (authMode == AuthFormMode.REGISTER) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text(stringResource(R.string.ui_your_name_f48db7), fontSize = 13.sp, color = hInkMuted) },
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
                                placeholder = { Text(stringResource(R.string.ui_email_84add5), fontSize = 13.sp, color = hInkMuted) },
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
                                    placeholder = { Text(stringResource(R.string.ui_password_8be3c9), fontSize = 13.sp, color = hInkMuted) },
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
                                        AuthFormMode.REGISTER -> onRegister(email.trim(), password, name.trim())
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
                                            AuthFormMode.LOGIN -> stringResource(R.string.ui_sign_in_f8492c)
                                            AuthFormMode.REGISTER -> stringResource(R.string.ui_create_account_eff4fd)
                                            AuthFormMode.FORGOT -> stringResource(R.string.ui_send_recovery_link_f3b6cf)
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
                                        text = if (authMode == AuthFormMode.LOGIN) stringResource(R.string.ui_don_t_have_an_account_sign_up_e7eacd) else stringResource(R.string.ui_already_have_an_account_sign_in_6603c2),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = hGold
                                    )
                                }

                                if (authMode != AuthFormMode.FORGOT) {
                                    Text(
                                        text = stringResource(R.string.ui_forgot_password_1cc8d1),
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
                                text = stringResource(R.string.ui_backup_restore_3bdd40),
                                fontFamily = fontFamilyUi,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = stringResource(R.string.ui_last_synced_1_s_9e8bda, com.nur.quran.i18n.relativeTime(lastSyncAt)),
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                color = hInkMuted
                            )
                        }
                    }

                    // Live Status Row
                    if (syncStatusActive) {
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
                                    text = syncStatusMessage ?: stringResource(R.string.ui_syncing_data_8b9e1e),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hGold
                                )
                            } else if (isSyncSuccess) {
                                Text(
                                    text = stringResource(R.string.ui_sync_complete_successfully_20bf26),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            } else if (syncError != null) {
                                Text(
                                    text = syncError,
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
                            enabled = !syncStatusActive,
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
                                Text(stringResource(R.string.ui_restore_3cbe6d), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Backup Button
                        Button(
                            onClick = onBackup,
                            enabled = !syncStatusActive,
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
                                Text(stringResource(R.string.ui_backup_dd9699), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
