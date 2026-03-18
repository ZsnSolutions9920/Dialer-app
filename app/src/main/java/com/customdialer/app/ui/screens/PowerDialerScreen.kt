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
import com.customdialer.app.data.model.PhoneList
import com.customdialer.app.data.model.PhoneListEntry
import com.customdialer.app.data.model.PowerDialProgress
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PowerDialerScreen(
    phoneLists: List<PhoneList>,
    selectedList: PhoneList?,
    entries: List<PhoneListEntry>,
    progress: PowerDialProgress?,
    isLoading: Boolean,
    onSelectList: (PhoneList) -> Unit,
    onBackToLists: () -> Unit,
    onDialEntry: (PhoneListEntry) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onDeleteList: (Int) -> Unit,
    onAutoDialNext: () -> Unit
) {
    val C = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
    ) {
        if (selectedList == null) {
            // Phone Lists view
            Text(
                "Phone Lists",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = C.textPrimary,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(phoneLists) { list ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = C.card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectList(list) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    list.name ?: "Unnamed List",
                                    fontWeight = FontWeight.Medium,
                                    color = C.textPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${list.totalEntries ?: 0} entries",
                                    fontSize = 13.sp,
                                    color = C.textSecondary
                                )
                                list.description?.let {
                                    Text(it, fontSize = 12.sp, color = C.textMuted)
                                }
                            }
                            Row {
                                IconButton(onClick = { onSelectList(list) }) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = "Open", tint = PrimaryBlue)
                                }
                                IconButton(onClick = { onDeleteList(list.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (phoneLists.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = C.textMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No phone lists", color = C.textMuted)
                                Text("Upload lists from the web app", fontSize = 12.sp, color = C.textMuted)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            // List entries view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLists) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = C.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedList.name ?: "Phone List",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.textPrimary
                    )
                }
                Button(
                    onClick = onAutoDialNext,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Dial")
                }
            }

            // Progress bar
            if (progress != null) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = C.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progress", fontSize = 13.sp, color = C.textSecondary)
                            Text(
                                "${progress.called}/${progress.total} (${String.format("%.0f", progress.percentage)}%)",
                                fontSize = 13.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (progress.percentage / 100).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue,
                            trackColor = C.surfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Entries
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries) { entry ->
                    PhoneListEntryItem(
                        entry = entry,
                        onDial = { onDialEntry(entry) },
                        onUpdateStatus = { status -> onUpdateStatus(entry.id, status) }
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

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun PhoneListEntryItem(
    entry: PhoneListEntry,
    onDial: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val C = AppColors
    val statusColor = when (entry.status) {
        "called" -> AccentGreen
        "no_answer" -> AccentOrange
        "follow_up" -> AccentCyan
        "not_interested" -> AccentRed
        "do_not_contact" -> AccentRed
        else -> C.textMuted // pending
    }

    var showStatusMenu by remember { mutableStateOf(false) }
    val statuses = listOf("pending", "called", "no_answer", "follow_up", "not_interested", "do_not_contact")

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = C.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name ?: entry.phoneNumber ?: "Unknown",
                    fontWeight = FontWeight.Medium,
                    color = C.textPrimary,
                    fontSize = 14.sp
                )
                Text(
                    entry.phoneNumber ?: "",
                    fontSize = 12.sp,
                    color = C.textSecondary
                )
            }
            // Status chip
            Box {
                TextButton(onClick = { showStatusMenu = true }) {
                    Text(
                        entry.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Pending",
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                    statuses.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onUpdateStatus(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onDial, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Call, contentDescription = "Dial", tint = AccentGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}
