package com.customdialer.app.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.ui.theme.*

@Composable
fun IncomingCallOverlay(
    fromNumber: String,
    callerName: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val C = AppColors

    // Pulsing animation for the call icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = C.card),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing phone icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Incoming Call",
                    fontSize = 14.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Caller name
                if (!callerName.isNullOrBlank()) {
                    Text(
                        callerName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Phone number
                Text(
                    fromNumber,
                    fontSize = if (callerName.isNullOrBlank()) 24.sp else 16.sp,
                    fontWeight = if (callerName.isNullOrBlank()) FontWeight.Bold else FontWeight.Normal,
                    color = if (callerName.isNullOrBlank()) C.textPrimary else C.textSecondary
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Accept / Reject buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reject
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentRed)
                        ) {
                            Icon(
                                Icons.Filled.CallEnd,
                                contentDescription = "Reject",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", fontSize = 13.sp, color = AccentRed)
                    }

                    // Accept
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        ) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", fontSize = 13.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}
