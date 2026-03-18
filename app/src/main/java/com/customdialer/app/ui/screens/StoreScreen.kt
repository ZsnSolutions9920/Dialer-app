package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.data.model.AvailableNumber
import com.customdialer.app.data.model.MinutesPackage
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    availableNumbers: List<AvailableNumber>,
    numbersLoading: Boolean,
    selectedNumberType: String,
    onNumberTypeChange: (String) -> Unit,
    onRefreshNumbers: () -> Unit,
    minutesPackages: List<MinutesPackage>,
    packagesLoading: Boolean,
    onBuyPackage: (MinutesPackage) -> Unit,
    onBuyNumber: (AvailableNumber) -> Unit,
    onMockBuyPackage: (MinutesPackage) -> Unit,
    onMockBuyNumber: (AvailableNumber) -> Unit,
    hasCard: Boolean,
    purchaseMessage: String?
) {
    val C = AppColors
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Phone Numbers", "Minutes")

    // Confirmation dialog state
    var confirmMinutes by remember { mutableStateOf<MinutesPackage?>(null) }
    var confirmNumber by remember { mutableStateOf<AvailableNumber?>(null) }

    // Confirm minutes purchase dialog
    if (confirmMinutes != null) {
        AlertDialog(
            onDismissRequest = { confirmMinutes = null },
            title = { Text("Confirm Purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Buy ${confirmMinutes!!.minutes} minutes for $${String.format("%.2f", confirmMinutes!!.price)}?")
                    if (!hasCard) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No card on file. This will be a test purchase.", fontSize = 12.sp, color = C.textMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hasCard) onBuyPackage(confirmMinutes!!) else onMockBuyPackage(confirmMinutes!!)
                        confirmMinutes = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text(if (hasCard) "Pay Now" else "Test Purchase") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMinutes = null }) { Text("Cancel") }
            }
        )
    }

    // Confirm number purchase dialog
    if (confirmNumber != null) {
        AlertDialog(
            onDismissRequest = { confirmNumber = null },
            title = { Text("Confirm Purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Buy ${confirmNumber!!.phoneNumber} for $${String.format("%.2f", confirmNumber!!.monthlyPrice ?: 0.0)}/month?")
                    if (!hasCard) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No card on file. This will be a test purchase.", fontSize = 12.sp, color = C.textMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hasCard) onBuyNumber(confirmNumber!!) else onMockBuyNumber(confirmNumber!!)
                        confirmNumber = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text(if (hasCard) "Pay Now" else "Test Purchase") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNumber = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Purchase message banner
        if (purchaseMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(purchaseMessage, color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Tabs
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.surface)
                    .border(1.dp, C.border, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == index) PrimaryBlue else C.surface)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (selectedTab == index) androidx.compose.ui.graphics.Color.White else C.textSecondary
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Number type filter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("local" to "Local", "tollfree" to "Toll-Free").forEach { (value, label) ->
                            FilterChip(
                                selected = selectedNumberType == value,
                                onClick = { onNumberTypeChange(value) },
                                label = { Text(label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onRefreshNumbers) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = PrimaryBlue
                            )
                        }
                    }
                }

                if (numbersLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Fetching available numbers...", color = C.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (availableNumbers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = C.textMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No numbers loaded", color = C.textSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap refresh to browse numbers", color = C.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(availableNumbers) { number ->
                        NumberCard(number = number, onBuy = { confirmNumber = number })
                    }
                }
            }

            1 -> {
                // Minutes packages header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Buy Calling Minutes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = C.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "The more you buy, the more you save!",
                                fontSize = 14.sp,
                                color = C.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (packagesLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    items(minutesPackages) { pkg ->
                        MinutesPackageCard(pkg = pkg, onBuy = { confirmMinutes = pkg })
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun NumberCard(number: AvailableNumber, onBuy: () -> Unit) {
    val C = AppColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = C.card),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ThemeState.isDarkMode.value) 0.dp else 2.dp),
        border = if (!ThemeState.isDarkMode.value) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Number details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    number.phoneNumber ?: "",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = C.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (number.locality != null || number.region != null) {
                        Text(
                            listOfNotNull(number.locality, number.region).joinToString(", "),
                            fontSize = 12.sp,
                            color = C.textSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    val type = if (number.type == "tollfree") "Toll-Free" else "Local"
                    Text(
                        type,
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // Capabilities
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (number.capabilities?.voice == true) CapBadge("Voice")
                    if (number.capabilities?.sms == true) CapBadge("SMS")
                    if (number.capabilities?.mms == true) CapBadge("MMS")
                }
            }

            // Price + buy
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", number.monthlyPrice ?: 0.0)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AccentGreen
                )
                Text("/month", fontSize = 10.sp, color = C.textMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onBuy,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text("Buy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CapBadge(label: String) {
    val C = AppColors
    Text(
        label,
        fontSize = 10.sp,
        color = C.textMuted,
        modifier = Modifier
            .border(1.dp, C.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

@Composable
private fun MinutesPackageCard(pkg: MinutesPackage, onBuy: () -> Unit) {
    val C = AppColors
    val isBestValue = pkg.id == "min_1500"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBestValue) PrimaryBlue.copy(alpha = 0.08f) else C.card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ThemeState.isDarkMode.value) 0.dp else 2.dp),
        border = if (isBestValue) androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
                 else if (!ThemeState.isDarkMode.value) androidx.compose.foundation.BorderStroke(1.dp, C.border)
                 else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isBestValue) PrimaryBlue.copy(alpha = 0.15f)
                        else AccentOrange.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint = if (isBestValue) PrimaryBlue else AccentOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Package details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${pkg.minutes} Minutes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = C.textPrimary
                    )
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BEST VALUE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "$${String.format("%.3f", pkg.perMinute)}/min",
                    fontSize = 13.sp,
                    color = C.textSecondary
                )
                if (pkg.savings > 0) {
                    Text(
                        "Save ${pkg.savings}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentGreen
                    )
                }
            }

            // Price + buy
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", pkg.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = C.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onBuy() },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBestValue) PrimaryBlue else AccentGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text("Buy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
