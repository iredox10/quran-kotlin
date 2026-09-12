package com.nur.quran.ui.components.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Danger Zone section matching web app Profile.jsx lines 426-441.
 */
@Composable
fun ProfileDangerZone(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val redColor = Color(0xFFEF4444)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DANGER ZONE",
            fontFamily = fontFamilyMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = redColor.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, redColor.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            .background(redColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.LogOut,
                            contentDescription = null,
                            tint = redColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Sign Out",
                        fontFamily = fontFamilyBody,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = redColor
                    )
                }

                Icon(
                    imageVector = NurIcons.ChevronRight,
                    contentDescription = null,
                    tint = redColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
