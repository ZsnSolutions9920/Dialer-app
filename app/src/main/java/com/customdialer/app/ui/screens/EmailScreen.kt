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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.data.model.Email
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun EmailScreen(
    emails: List<Email>,
    unreadCount: Int,
    isLoading: Boolean,
    selectedFolder: String?,
    onFolderChange: (String?) -> Unit,
    onEmailClick: (Email) -> Unit,
    onSync: () -> Unit,
    onRefresh: () -> Unit
) {
    val C = AppColors
    val folders = listOf(null to "All", "inbox" to "Inbox", "sent" to "Sent")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Email",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.textPrimary
                )
                if (unreadCount > 0) {
                    Text(
                        "$unreadCount unread",
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                }
            }
            Row {
                IconButton(onClick = onSync) {
                    Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = PrimaryBlue)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = C.textSecondary)
                }
            }
        }

        // Folder tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.forEach { (value, label) ->
                FilterChip(
                    selected = selectedFolder == value,
                    onClick = { onFolderChange(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                        containerColor = C.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Email list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(emails) { email ->
                EmailItem(email = email, onClick = { onEmailClick(email) })
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

            if (emails.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.MailOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = C.textMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No emails", color = C.textMuted)
                            Text("Configure SMTP in the web app to start", fontSize = 12.sp, color = C.textMuted)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun EmailItem(email: Email, onClick: () -> Unit) {
    val C = AppColors
    val isUnread = email.isRead != true

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) C.card else C.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Unread indicator
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        email.fromEmail ?: "Unknown",
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = C.textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    email.createdAt?.let {
                        Text(
                            formatCallDate(it),
                            fontSize = 11.sp,
                            color = C.textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    email.subject ?: "(No subject)",
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isUnread) C.textPrimary else C.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    email.body?.take(80)?.replace(Regex("<[^>]*>"), "")?.replace("&nbsp;", " ") ?: "",
                    color = C.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
