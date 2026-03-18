package com.customdialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customdialer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentBaseUrl: String,
    onSaveBaseUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    val C = AppColors
    var baseUrl by remember { mutableStateOf(currentBaseUrl) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = C.textPrimary)
            }
            Text(
                "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = C.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Server settings
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = C.card)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Server Connection",
                    fontWeight = FontWeight.SemiBold,
                    color = C.textPrimary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Configure the backend server URL",
                    fontSize = 12.sp,
                    color = C.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        saved = false
                    },
                    label = { Text("Server URL") },
                    leadingIcon = { Icon(Icons.Filled.Cloud, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary,
                        unfocusedTextColor = C.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSaveBaseUrl(baseUrl)
                        saved = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
                if (saved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Saved! Restart the app for changes to take full effect.",
                        fontSize = 12.sp,
                        color = AccentGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = C.card)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold, color = C.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Custom Dialer v1.0.0", fontSize = 14.sp, color = C.textSecondary)
                Text("Android companion app for the Custom Dialer platform", fontSize = 12.sp, color = C.textMuted)
            }
        }
    }
}
