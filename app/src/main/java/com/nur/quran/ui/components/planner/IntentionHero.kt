package com.nur.quran.ui.components.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Header section for Intention mode matching web Planner.jsx lines 220-240.
 */
@Composable
fun IntentionHero(
    hasActivePlan: Boolean,
    onViewActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasActivePlan) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = hTeal,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewActive() }
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Continue my active plan",
                        fontFamily = fontFamilyBody,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Icon(
                        imageVector = NurIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quran icon watermark (matching web: img /logo-192.png opacity-20)
        Icon(
            imageVector = NurIcons.BookOpen,
            contentDescription = null,
            tint = hInk.copy(alpha = 0.18f),
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "THE FIRST STEP",
            fontFamily = fontFamilyMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = hTeal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Set Your Intention",
            fontFamily = fontFamilyUi,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = hInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a pace that resonates with your soul. Whether intensive or slow, the journey of the Quran is a dialogue of devotion.",
            fontFamily = fontFamilyBody,
            fontSize = 13.5.sp,
            lineHeight = 22.sp,
            color = hInkMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
