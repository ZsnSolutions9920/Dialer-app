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
import com.customdialer.app.data.model.Contact
import com.customdialer.app.data.model.ContactCreate
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onDialContact: (String) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onDeleteContact: (Int) -> Unit,
    onCreateContact: (ContactCreate) -> Unit
) {
    val C = AppColors
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(C.background)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search contacts...") },
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

            // Contact list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onDial = { contact.phoneNumber?.let { onDialContact(it) } },
                        onFavorite = { onToggleFavorite(contact.id) },
                        onDelete = { onDeleteContact(contact.id) }
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

                if (contacts.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load More", color = PrimaryBlue)
                        }
                    }
                }

                if (contacts.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Contacts,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = C.textMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No contacts found", color = C.textMuted)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = PrimaryBlue,
            contentColor = androidx.compose.ui.graphics.Color.White
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = "Add Contact")
        }
    }

    // Create contact dialog
    if (showCreateDialog) {
        CreateContactDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = {
                onCreateContact(it)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit,
    onDial: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val C = AppColors
    var showMenu by remember { mutableStateOf(false) }

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
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (contact.name?.firstOrNull()?.uppercase() ?: "?"),
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        contact.name ?: "Unknown",
                        fontWeight = FontWeight.Medium,
                        color = C.textPrimary,
                        fontSize = 15.sp
                    )
                    if (contact.isFavorite == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = AccentYellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    contact.phoneNumber ?: "",
                    fontSize = 13.sp,
                    color = C.textSecondary
                )
                if (!contact.company.isNullOrBlank()) {
                    Text(contact.company, fontSize = 11.sp, color = C.textMuted)
                }
            }

            // Call button
            IconButton(onClick = onDial) {
                Icon(Icons.Filled.Call, contentDescription = "Call", tint = AccentGreen, modifier = Modifier.size(20.dp))
            }

            // More options
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = C.textSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (contact.isFavorite == true) "Unfavorite" else "Favorite") },
                        onClick = { onFavorite(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                if (contact.isFavorite == true) Icons.Filled.StarBorder else Icons.Filled.Star,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AccentRed) },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AccentRed) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContactDialog(
    onDismiss: () -> Unit,
    onCreate: (ContactCreate) -> Unit
) {
    val C = AppColors
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onCreate(
                            ContactCreate(
                                name = name,
                                phoneNumber = phone,
                                email = email.ifBlank { null },
                                company = company.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Create", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.textSecondary)
            }
        },
        title = { Text("New Contact", color = C.textPrimary) },
        containerColor = C.card,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary
                    )
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary
                    )
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary
                    )
                )
                OutlinedTextField(
                    value = company, onValueChange = { company = it },
                    label = { Text("Company") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary
                    )
                )
            }
        }
    )
}
