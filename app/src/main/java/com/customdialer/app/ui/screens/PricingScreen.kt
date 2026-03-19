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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.data.model.SubPackage
import com.customdialer.app.ui.theme.*

@Composable
fun PricingScreen(
    packages: List<SubPackage>,
    currentPackage: String,
    onBuyPackage: (String) -> Unit,
    purchaseMessage: String?,
    onBack: (() -> Unit)? = null
) {
    val C = AppColors
    val isDark = ThemeState.isDarkMode.value

    var confirmPkg by remember { mutableStateOf<SubPackage?>(null) }

    // Confirm dialog
    if (confirmPkg != null) {
        AlertDialog(
            onDismissRequest = { confirmPkg = null },
            title = { Text("Confirm Subscription", fontWeight = FontWeight.Bold) },
            text = {
                Text("Subscribe to ${confirmPkg!!.name} for $${String.format("%.2f", confirmPkg!!.price)}/${confirmPkg!!.period ?: "month"}?")
            },
            confirmButton = {
                Button(
                    onClick = { onBuyPackage(confirmPkg!!.id); confirmPkg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Subscribe") }
            },
            dismissButton = {
                TextButton(onClick = { confirmPkg = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onBack != null) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = C.textPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose Your Plan",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Unlock features to grow your business",
                    fontSize = 14.sp,
                    color = C.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Purchase message
        if (purchaseMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(purchaseMessage, color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Package cards
        items(packages) { pkg ->
            val isPopular = pkg.popular == true
            val isCurrent = pkg.id == currentPackage
            val pkgColor = when (pkg.id) {
                "basic" -> PrimaryBlue
                "silver" -> Color(0xFF7c8db5)
                "premium" -> Color(0xFFffa726)
                else -> PrimaryBlue
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) pkgColor.copy(alpha = 0.06f) else C.card
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isPopular) 6.dp else if (isDark) 0.dp else 2.dp
                ),
                border = when {
                    isCurrent -> androidx.compose.foundation.BorderStroke(2.dp, pkgColor)
                    isPopular -> androidx.compose.foundation.BorderStroke(1.5.dp, pkgColor.copy(alpha = 0.4f))
                    !isDark -> androidx.compose.foundation.BorderStroke(1.dp, C.border)
                    else -> null
                }
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pkgColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (pkg.id) {
                                        "basic" -> Icons.Filled.Phone
                                        "silver" -> Icons.Filled.Contacts
                                        "premium" -> Icons.Filled.Star
                                        else -> Icons.Filled.Phone
                                    },
                                    contentDescription = null,
                                    tint = pkgColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = C.textPrimary)
                                if (isCurrent) {
                                    Text("Current Plan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pkgColor)
                                }
                            }
                        }
                        if (isPopular && !isCurrent) {
                            Text(
                                "POPULAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pkgColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(pkgColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$${String.format("%.2f", pkg.price)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.textPrimary
                        )
                        Text(
                            "/${pkg.period ?: "mo"}",
                            fontSize = 14.sp,
                            color = C.textMuted,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pkg.description ?: "", fontSize = 13.sp, color = C.textSecondary)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Features
                    pkg.features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(feature, fontSize = 14.sp, color = C.textPrimary)
                        }
                    }
                    pkg.lockedFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = C.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                feature,
                                fontSize = 14.sp,
                                color = C.textMuted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buy button
                    if (isCurrent) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = false
                        ) {
                            Text("Current Plan", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { confirmPkg = pkg },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pkgColor)
                        ) {
                            Text(
                                if (currentPackage == "free") "Get Started" else "Upgrade",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
