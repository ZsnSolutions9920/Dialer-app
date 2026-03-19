package com.customdialer.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.ui.theme.*

@Composable
fun KycPendingScreen(
    onRefreshStatus: () -> Unit,
    onLogout: () -> Unit,
    isChecking: Boolean
) {
    val C = AppColors

    // Rotating hourglass animation
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated hourglass
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AccentOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.HourglassTop,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier
                    .size(56.dp)
                    .rotate(rotation)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Verification In Progress",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = C.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Your KYC documents have been submitted successfully and are being reviewed by our team.",
            fontSize = 15.sp,
            color = C.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Estimated Wait Time",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = C.textSecondary
                )
                Text(
                    "Up to 1 Hour",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You will be able to access the app once your identity is verified. Please check back shortly.",
                    fontSize = 13.sp,
                    color = C.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Check status button
        Button(
            onClick = onRefreshStatus,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Checking...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Status", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout option
        TextButton(onClick = onLogout) {
            Text("Sign out", color = C.textMuted, fontSize = 14.sp)
        }
    }
}
