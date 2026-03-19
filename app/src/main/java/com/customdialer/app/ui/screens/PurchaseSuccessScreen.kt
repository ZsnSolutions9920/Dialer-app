package com.customdialer.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.ui.theme.*

@Composable
fun PurchaseSuccessScreen(
    onGoToDashboard: () -> Unit,
    onGoToStore: () -> Unit
) {
    val C = AppColors

    // Animate the checkmark scaling in
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = AccentGreen,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Purchase Successful!",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = C.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Your purchase has been completed successfully. Your account has been updated with the new features.",
            fontSize = 15.sp,
            color = C.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Go to Dashboard button
        Button(
            onClick = onGoToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Go to Dashboard", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue Shopping button
        OutlinedButton(
            onClick = onGoToStore,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue Shopping", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
