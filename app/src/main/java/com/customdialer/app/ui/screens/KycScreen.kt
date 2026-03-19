package com.customdialer.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.customdialer.app.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(
    onSubmit: (fullName: String, cnicUri: Uri, selfieUri: Uri) -> Unit,
    isLoading: Boolean,
    error: String?,
    rejectionMessage: String?,
    onLogout: () -> Unit
) {
    val C = AppColors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var fullName by remember { mutableStateOf("") }
    var cnicImageUri by remember { mutableStateOf<Uri?>(null) }
    var selfieImageUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    // CNIC image — allow gallery or camera
    var showCnicOptions by remember { mutableStateOf(false) }

    // Temp file URI for camera captures
    var cnicCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selfieCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun createTempImageUri(prefix: String): Uri {
        val file = File.createTempFile(prefix, ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // CNIC from gallery
    val cnicGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) cnicImageUri = uri }

    // CNIC from camera
    val cnicCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && cnicCameraUri != null) cnicImageUri = cnicCameraUri }

    // Selfie — camera only
    val selfieCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && selfieCameraUri != null) selfieImageUri = selfieCameraUri }

    // Camera permission
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Re-trigger the selfie camera after permission
            val uri = createTempImageUri("selfie_")
            selfieCameraUri = uri
            selfieCameraLauncher.launch(uri)
        }
    }

    // CNIC option dialog
    if (showCnicOptions) {
        AlertDialog(
            onDismissRequest = { showCnicOptions = false },
            title = { Text("Upload CNIC Image", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how to add your CNIC/ID card image") },
            confirmButton = {
                TextButton(onClick = {
                    showCnicOptions = false
                    val uri = createTempImageUri("cnic_")
                    cnicCameraUri = uri
                    cnicCameraLauncher.launch(uri)
                }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCnicOptions = false
                    cnicGalleryLauncher.launch("image/*")
                }) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Identity Verification",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = C.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Please complete KYC verification to activate your account",
            fontSize = 14.sp,
            color = C.textSecondary,
            textAlign = TextAlign.Center
        )

        // Rejection message
        if (rejectionMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Previous submission rejected", color = AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(rejectionMessage, color = C.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Step indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = C.card),
            elevation = CardDefaults.cardElevation(defaultElevation = if (ThemeState.isDarkMode.value) 0.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Step 1: Full Name
                Text(
                    "Step 1: Full Legal Name",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = C.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; localError = null },
                    label = { Text("Full Name (as on CNIC)") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = C.border,
                        focusedTextColor = C.textPrimary,
                        unfocusedTextColor = C.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = C.border)
                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: CNIC Image
                Text(
                    "Step 2: CNIC / ID Card Image",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = C.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Upload a clear photo of the front of your CNIC or national ID card",
                    fontSize = 12.sp,
                    color = C.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                ImagePickerBox(
                    imageUri = cnicImageUri,
                    icon = Icons.Filled.Badge,
                    label = "Tap to upload CNIC image",
                    onClick = { showCnicOptions = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = C.border)
                Spacer(modifier = Modifier.height(20.dp))

                // Step 3: Live Selfie
                Text(
                    "Step 3: Live Selfie",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = C.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Take a live photo of yourself. This must be captured now using your camera.",
                    fontSize = 12.sp,
                    color = C.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                ImagePickerBox(
                    imageUri = selfieImageUri,
                    icon = Icons.Filled.Face,
                    label = "Tap to take a selfie",
                    onClick = {
                        val uri = createTempImageUri("selfie_")
                        selfieCameraUri = uri
                        // Check camera permission
                        val hasCamPerm = context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCamPerm) {
                            selfieCameraLauncher.launch(uri)
                        } else {
                            cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        val displayError = localError ?: error
        if (displayError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayError, color = AccentRed, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Submit button
        Button(
            onClick = {
                when {
                    fullName.isBlank() -> localError = "Please enter your full name"
                    cnicImageUri == null -> localError = "Please upload your CNIC image"
                    selfieImageUri == null -> localError = "Please take a selfie"
                    else -> {
                        localError = null
                        onSubmit(fullName, cnicImageUri!!, selfieImageUri!!)
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Submitting...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit for Verification", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout option
        TextButton(onClick = onLogout) {
            Text("Sign out", color = C.textMuted, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ImagePickerBox(
    imageUri: Uri?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val C = AppColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (imageUri != null) 200.dp else 140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (imageUri != null) AccentGreen.copy(alpha = 0.5f) else C.border,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (imageUri != null) AccentGreen.copy(alpha = 0.05f)
                else C.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Overlay with "change" icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Change",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Success badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGreen.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uploaded", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = C.textMuted,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(label, color = C.textSecondary, fontSize = 13.sp)
            }
        }
    }
}
