package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.data.model.*
import com.customdialer.app.ui.theme.*

@Composable
fun ProfileScreen(
    agent: Agent?,
    attendance: AttendanceSession?,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    customerName: String?,
    customerEmail: String?,
    myMinutes: MinutesSummary?,
    myNumbers: List<MyNumber>,
    purchaseHistory: List<Purchase>,
    paymentMethod: PaymentMethodInfo?,
    onAddCard: () -> Unit
) {
    val C = AppColors
    val isDark = ThemeState.isDarkMode.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar and name
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (customerName ?: customerEmail ?: agent?.displayName ?: "?")
                            .take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    customerName ?: agent?.displayName ?: "User",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.textPrimary
                )
                Text(
                    customerEmail ?: agent?.username ?: "",
                    fontSize = 14.sp,
                    color = C.textSecondary
                )
            }
        }

        // My Minutes & Numbers summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minutes card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${myMinutes?.remainingMinutes ?: 0}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.textPrimary
                        )
                        Text("Minutes Left", fontSize = 12.sp, color = C.textSecondary)
                    }
                }

                // Numbers card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${myNumbers.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.textPrimary
                        )
                        Text("Phone Numbers", fontSize = 12.sp, color = C.textSecondary)
                    }
                }
            }
        }

        // Payment Method
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = C.card),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (paymentMethod?.hasCard == true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGreen.copy(alpha = 0.08f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${(paymentMethod.brand ?: "Card").replaceFirstChar { it.uppercase() }} ending in ${paymentMethod.last4}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = C.textPrimary
                                )
                                Text("Ready for purchases", fontSize = 12.sp, color = AccentGreen)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onAddCard,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Update Card", color = PrimaryBlue)
                        }
                    } else {
                        Text(
                            "Add a credit card to purchase minutes and phone numbers.",
                            fontSize = 13.sp,
                            color = C.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = onAddCard,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Credit Card", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // My Phone Numbers
        if (myNumbers.isNotEmpty()) {
            item {
                Text("My Numbers", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.textPrimary)
            }
            items(myNumbers) { num ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(num.phoneNumber ?: "", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.textPrimary)
                            Text(
                                "${if (num.type == "tollfree") "Toll-Free" else "Local"} - $${String.format("%.2f", num.monthlyPrice ?: 0.0)}/mo",
                                fontSize = 12.sp, color = C.textSecondary
                            )
                        }
                        Text(
                            num.status?.replaceFirstChar { it.uppercase() } ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (num.status == "active") AccentGreen else AccentRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (num.status == "active") AccentGreen.copy(alpha = 0.1f)
                                    else AccentRed.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Purchase History
        if (purchaseHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Purchase History", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.textPrimary)
            }
            items(purchaseHistory.take(10)) { p ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (p.type == "minutes") PrimaryBlue.copy(alpha = 0.12f)
                                    else AccentOrange.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (p.type == "minutes") Icons.Filled.Timer else Icons.Filled.Phone,
                                contentDescription = null,
                                tint = if (p.type == "minutes") PrimaryBlue else AccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.itemLabel ?: p.type ?: "", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.textPrimary)
                            Row {
                                if (p.mock == true) {
                                    Text("Mock", fontSize = 11.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                                    Text(" - ", fontSize = 11.sp, color = C.textMuted)
                                }
                                Text(
                                    p.status?.replaceFirstChar { it.uppercase() } ?: "",
                                    fontSize = 11.sp,
                                    color = if (p.status == "completed") AccentGreen else C.textMuted
                                )
                            }
                        }
                        Text(
                            "$${String.format("%.2f", p.amount ?: 0.0)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = C.textPrimary
                        )
                    }
                }
            }
        }

        // Actions
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = C.card),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
            ) {
                Column {
                    TextButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = C.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Settings", color = C.textPrimary, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = C.textMuted)
                    }
                    Divider(color = C.border)
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign Out", color = AccentRed, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val C = AppColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = C.textMuted)
            Text(value, fontSize = 14.sp, color = C.textPrimary)
        }
    }
}
