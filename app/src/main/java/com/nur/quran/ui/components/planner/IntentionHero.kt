package com.nur.quran.ui.components.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                shape = RoundedCornerShape(16.dp),
                color = hTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewActive() }
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Continue my active plan",
                        fontFamily = fontFamilyBody,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = hWhite
                    )
                    Icon(
                        imageVector = NurIcons.ArrowRight,
                        contentDescription = null,
                        tint = hWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "THE FIRST STEP",
            fontFamily = fontFamilyMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = hTeal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Set Your Intention",
            fontFamily = fontFamilyUi,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = hInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a pace that resonates with your soul. Whether intensive or slow, the journey of the Quran is a dialogue of devotion.",
            fontFamily = fontFamilyBody,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = hInkMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
