package com.nur.quran.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

/**
 * Footer matching web app Profile.jsx lines 443-448.
 */
@Composable
fun ProfileFooter(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "QURAN NUR · V1.0.0",
            fontFamily = fontFamilyMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = hInkMuted
        )
        Text(
            text = "Made with ♥ for the Ummah",
            fontFamily = fontFamilyUi,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = hInkMuted.copy(alpha = 0.8f)
        )
    }
}
