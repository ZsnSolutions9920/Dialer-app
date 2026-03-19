package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.data.model.*
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    customerName: String?,
    callingStatus: CallingStatus?,
    recentCalls: List<CallLog>,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    onNavigateToStore: () -> Unit
) {
    val C = AppColors
    val isDark = ThemeState.isDarkMode.value
    val canCall = callingStatus?.canMakeCalls == true
    val minutesRemaining = callingStatus?.minutesRemaining ?: 0
    val minutesTotal = callingStatus?.minutesTotal ?: 0
    val minutesUsed = callingStatus?.minutesUsed ?: 0
    val minutesPercent = if (minutesTotal > 0) (minutesRemaining.toFloat() / minutesTotal) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good ${getGreeting()},", fontSize = 14.sp, color = C.textSecondary)
                    Text(
                        customerName?.split(" ")?.firstOrNull() ?: "there",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) C.surfaceVariant else PrimaryBlue.copy(alpha = 0.08f))
                        .then(
                            if (!isDark) Modifier.border(1.dp, PrimaryBlue.copy(alpha = 0.15f), CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // ── Main Balance Card ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Minutes Balance", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$minutesRemaining",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "min",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(minutesPercent.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${minutesUsed} used", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text("${minutesTotal} total", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // ── Quick Stats Row ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Phone,
                    label = "Number",
                    value = if (callingStatus?.hasNumber == true)
                        callingStatus.phoneNumber?.takeLast(4)?.let { "...$it" } ?: "Active"
                    else "None",
                    color = if (callingStatus?.hasNumber == true) AccentGreen else C.textMuted,
                    isDark = isDark
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CallMade,
                    label = "Calls Made",
                    value = "${recentCalls.count { it.direction == "outbound" }}",
                    color = PrimaryBlue,
                    isDark = isDark
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    icon = if (canCall) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    label = "Status",
                    value = if (canCall) "Active" else "Setup",
                    color = if (canCall) AccentGreen else AccentOrange,
                    isDark = isDark
                )
            }
        }

        // ── Action Banner ──
        if (!canCall) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) AccentOrange.copy(alpha = 0.1f) else AccentOrange.copy(alpha = 0.06f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AccentOrange.copy(alpha = if (isDark) 0.2f else 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Get started",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = C.textPrimary
                            )
                            Text(
                                when {
                                    callingStatus?.pkg == "free" || callingStatus?.pkg == null -> "Choose a plan to unlock calling features"
                                    callingStatus.hasNumber != true -> "Purchase a phone number to start calling"
                                    minutesRemaining <= 0 -> "Buy minutes to make your first call"
                                    else -> "Complete your setup"
                                },
                                fontSize = 12.sp,
                                color = C.textSecondary
                            )
                        }
                        Button(
                            onClick = onNavigateToStore,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (callingStatus?.pkg == "free" || callingStatus?.pkg == null) "Plans" else "Store",
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Recent Calls ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Calls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.textPrimary
                )
                if (recentCalls.isNotEmpty()) {
                    Text(
                        "${recentCalls.size} calls",
                        fontSize = 13.sp,
                        color = C.textMuted
                    )
                }
            }
        }

        if (recentCalls.isEmpty() && !isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(C.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = C.textMuted, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No calls yet", fontWeight = FontWeight.SemiBold, color = C.textPrimary, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Your call history will appear here\nonce you start making calls",
                            color = C.textMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Call list inside a single card
        if (recentCalls.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Column {
                        recentCalls.take(10).forEachIndexed { index, call ->
                            if (index > 0) {
                                Divider(
                                    color = C.border,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            CallRow(call = call)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun CallRow(call: CallLog) {
    val C = AppColors
    val isOutbound = call.direction == "outbound"
    val phoneNumber = if (isOutbound) call.toNumber else call.fromNumber
    val statusColor = when (call.status) {
        "completed" -> AccentGreen
        "no-answer", "busy" -> AccentOrange
        "failed", "canceled" -> AccentRed
        else -> C.textMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isOutbound) PrimaryBlue.copy(alpha = 0.1f)
                    else AccentGreen.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isOutbound) Icons.Filled.CallMade else Icons.Filled.CallReceived,
                contentDescription = null,
                tint = if (isOutbound) PrimaryBlue else AccentGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Number + details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                phoneNumber ?: "Unknown",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = C.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isOutbound) "Outbound" else "Inbound",
                    fontSize = 12.sp,
                    color = C.textMuted
                )
                if ((call.duration ?: 0) > 0) {
                    Text(" \u00B7 ", fontSize = 12.sp, color = C.textMuted)
                    Text(
                        formatDuration(call.duration ?: 0),
                        fontSize = 12.sp,
                        color = C.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Status pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                call.status?.replaceFirstChar { it.uppercase() } ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    isDark: Boolean
) {
    val C = AppColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = C.card),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.textPrimary,
                maxLines = 1
            )
            Text(label, fontSize = 11.sp, color = C.textMuted, maxLines = 1)
        }
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins >= 60) {
        val hrs = mins / 60
        val remMins = mins % 60
        "${hrs}h ${remMins}m"
    } else {
        "$mins:${secs.toString().padStart(2, '0')}"
    }
}
