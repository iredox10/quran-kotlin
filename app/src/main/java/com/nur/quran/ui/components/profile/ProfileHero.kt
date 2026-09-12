package com.nur.quran.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Premium Hero Avatar matching web app Profile.jsx lines 144-171.
 */
@Composable
fun ProfileHero(
    signedIn: Boolean,
    email: String?,
    displayName: String?,
    greeting: String,
    modifier: Modifier = Modifier
) {
    val initials = if (signedIn) {
        ProfileUtils.getInitials(displayName ?: email)
    } else null

    val title = if (signedIn) {
        displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
            ?: "Quran Student"
    } else greeting

    val subtitle = if (signedIn) {
        email ?: ""
    } else "Your personal Quran companion"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow halo
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(hGold.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(hSurface, hCream)
                        )
                    )
                    .border(2.dp, hGold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (initials != null) {
                    Text(
                        text = initials,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInk
                    )
                } else {
                    Icon(
                        imageVector = NurIcons.User,
                        contentDescription = "Guest",
                        tint = hInkMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upper category tag
            Text(
                text = "YOUR PROFILE",
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = hInkMuted
            )

            // Name or Greeting
            Text(
                text = title,
                fontFamily = fontFamilyUi,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                textAlign = TextAlign.Center
            )

            // Email or Subtitle
            Text(
                text = subtitle,
                fontFamily = fontFamilyBody,
                fontSize = 13.sp,
                color = hInkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
