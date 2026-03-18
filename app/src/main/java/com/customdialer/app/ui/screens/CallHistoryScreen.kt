package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.customdialer.app.data.model.CallLog
import com.customdialer.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun CallHistoryScreen(
    calls: List<CallLog>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onCallClick: (CallLog) -> Unit,
    onDialNumber: (String) -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    val C = AppColors
    val filters = listOf(null to "All", "outbound" to "Outbound", "inbound" to "Inbound")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search calls...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = C.border,
                focusedTextColor = C.textPrimary,
                unfocusedTextColor = C.textPrimary,
                focusedContainerColor = C.card,
                unfocusedContainerColor = C.card
            )
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (value, label) ->
                FilterChip(
                    selected = selectedFilter == value,
                    onClick = { onFilterChange(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                        containerColor = C.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Call list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(calls) { call ->
                CallLogItem(
                    call = call,
                    onClick = { onCallClick(call) },
                    onDial = { call.toNumber?.let { onDialNumber(it) } ?: call.fromNumber?.let { onDialNumber(it) } }
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }

            if (calls.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load More", color = PrimaryBlue)
                    }
                }
            }

            if (calls.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = C.textMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No calls found", color = C.textMuted)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CallLogItem(
    call: CallLog,
    onClick: () -> Unit,
    onDial: () -> Unit
) {
    val C = AppColors
    val isOutbound = call.direction == "outbound"
    val phoneNumber = if (isOutbound) call.toNumber else call.fromNumber
    val statusColor = when (call.status) {
        "completed" -> AccentGreen
        "no-answer", "missed" -> AccentRed
        "busy" -> AccentOrange
        else -> C.textSecondary
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = C.card),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOutbound) PrimaryBlue.copy(alpha = 0.15f)
                        else AccentCyan.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOutbound) Icons.Filled.CallMade else Icons.Filled.CallReceived,
                    contentDescription = null,
                    tint = if (isOutbound) PrimaryBlue else AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    phoneNumber ?: "Unknown",
                    fontWeight = FontWeight.Medium,
                    color = C.textPrimary,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        call.status?.replaceFirstChar { it.uppercase() } ?: "",
                        fontSize = 12.sp,
                        color = C.textSecondary
                    )
                    if (call.duration != null && call.duration > 0) {
                        Text(
                            " - ${formatDuration(call.duration)}",
                            fontSize = 12.sp,
                            color = C.textSecondary
                        )
                    }
                }
                call.createdAt?.let {
                    Text(
                        formatCallDate(it),
                        fontSize = 11.sp,
                        color = C.textMuted
                    )
                }
            }

            // Call back button
            IconButton(onClick = onDial) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Call",
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun formatCallDate(dateStr: String): String {
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        )
        val outFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
        for (fmt in formats) {
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            try {
                val date = fmt.parse(dateStr) ?: continue
                outFormat.timeZone = TimeZone.getDefault()
                return outFormat.format(date)
            } catch (_: Exception) { }
        }
        dateStr
    } catch (_: Exception) {
        dateStr
    }
}
