package com.nur.quran.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    // Animations
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrance animation
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
        delay(1400)
        // Exit animation
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
        )
        isVisible = false
        onFinish()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF004D40))
                .alpha(alphaAnim.value),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                // Logo with glowing halo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AEFECE4))
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Quran Nur Logo",
                        modifier = Modifier.size(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "QURAN NUR",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    letterSpacing = 4.sp,
                    color = Color(0xFFEFECE4)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ILLUMINATE YOUR HEART",
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    color = Color(0xFFC6A87C)
                )
            }
        }
    }
}
