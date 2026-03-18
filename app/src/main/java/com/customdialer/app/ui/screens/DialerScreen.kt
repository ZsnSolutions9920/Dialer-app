package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    onDial: (String) -> Unit,
    isOnCall: Boolean,
    activeCallNumber: String?,
    callStatus: String?,
    isMuted: Boolean,
    voiceReady: Boolean,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onSendDtmf: (String) -> Unit,
    myNumbers: List<com.customdialer.app.data.model.MyNumber> = emptyList(),
    activeNumber: String? = null,
    minutesRemaining: Int = 0,
    onSelectNumber: (String) -> Unit = {}
) {
    var phoneNumber by remember { mutableStateOf("") }
    var showNumberPicker by remember { mutableStateOf(false) }
    val C = AppColors
    val isDark = ThemeState.isDarkMode.value

    // Number picker dropdown
    if (showNumberPicker && myNumbers.size > 1) {
        AlertDialog(
            onDismissRequest = { showNumberPicker = false },
            title = { Text("Select Caller ID", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    myNumbers.forEach { num ->
                        val isSelected = num.phoneNumber == activeNumber
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    num.phoneNumber?.let { onSelectNumber(it) }
                                    showNumberPicker = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else C.surfaceVariant
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryBlue else C.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        num.phoneNumber ?: "",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = C.textPrimary
                                    )
                                    Text(
                                        if (num.type == "tollfree") "Toll-Free" else "Local",
                                        fontSize = 12.sp,
                                        color = C.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNumberPicker = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Caller ID + minutes banner
        if (!isOnCall) {
            if (activeNumber != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = myNumbers.size > 1) { showNumberPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, C.border) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calling from", fontSize = 10.sp, color = C.textMuted)
                            Text(activeNumber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.textPrimary)
                        }
                        Text(
                            "${minutesRemaining} min left",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (minutesRemaining > 10) AccentGreen else AccentRed
                        )
                        if (myNumbers.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.UnfoldMore, contentDescription = "Switch", tint = C.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Voice status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (voiceReady) AccentGreen else AccentOrange)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (voiceReady) "VoIP Ready" else if (activeNumber == null) "Purchase a number to start calling" else "Connecting to voice...",
                    fontSize = 12.sp,
                    color = if (voiceReady) AccentGreen else AccentOrange
                )
            }
        }

        // Active call banner
        if (isOnCall && activeCallNumber != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing call icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        activeCallNumber,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.textPrimary
                    )
                    callStatus?.let {
                        Text(it, fontSize = 14.sp, color = AccentGreen)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Call controls row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isMuted) AccentRed.copy(alpha = 0.2f)
                                        else C.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) AccentRed else C.textPrimary
                                )
                            }
                            Text(
                                if (isMuted) "Unmute" else "Mute",
                                fontSize = 11.sp,
                                color = C.textSecondary
                            )
                        }

                        // Hang up button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onHangUp,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed)
                            ) {
                                Icon(
                                    Icons.Filled.CallEnd,
                                    contentDescription = "Hang Up",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text("End", fontSize = 11.sp, color = C.textSecondary)
                        }

                        // Keypad/DTMF button (shows when on call)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { /* DTMF keypad is already visible */ },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(C.surfaceVariant)
                            ) {
                                Icon(
                                    Icons.Filled.Dialpad,
                                    contentDescription = "Keypad",
                                    tint = C.textPrimary
                                )
                            }
                            Text("Keypad", fontSize = 11.sp, color = C.textSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Phone number display (only when not on call)
        if (!isOnCall) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = C.card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (phoneNumber.isEmpty()) "Enter number" else phoneNumber,
                        fontSize = if (phoneNumber.length > 12) 24.sp else 30.sp,
                        fontWeight = FontWeight.Light,
                        color = if (phoneNumber.isEmpty()) C.textMuted else C.textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    if (phoneNumber.isNotEmpty()) {
                        IconButton(onClick = {
                            phoneNumber = phoneNumber.dropLast(1)
                        }) {
                            Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = C.textSecondary)
                        }
                    }
                }
            }
        }

        // Error/status message
        if (!isOnCall && callStatus != null) {
            Text(
                callStatus,
                fontSize = 13.sp,
                color = if (callStatus.startsWith("Failed")) AccentRed else AccentOrange,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dialpad
        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    DialpadKey(
                        digit = digit,
                        letters = letters,
                        onClick = {
                            if (isOnCall) {
                                onSendDtmf(digit)
                            } else {
                                phoneNumber += digit
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Call button (only when not on call)
        if (!isOnCall) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
                    .clickable {
                        if (phoneNumber.isNotBlank()) {
                            onDial(phoneNumber)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Call",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun DialpadKey(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    val C = AppColors
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(C.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = C.textPrimary
            )
            if (letters.isNotEmpty()) {
                Text(
                    letters,
                    fontSize = 10.sp,
                    color = C.textSecondary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
