package com.example.voicebillai.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.voicebillai.engine.OcrScannerEngine
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OcrScanModal(
    onDismiss: () -> Unit,
    onAddItems: (List<BillItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf("") }
    var recognizedItems by remember { mutableStateOf<List<BillItem>>(emptyList()) }
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processBitmap(bmp: Bitmap) {
        capturedBitmap = bmp
        isScanning = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val text = OcrScannerEngine.recognizeTextFromBitmap(bmp)
                extractedText = text
                if (text.isNotBlank()) {
                    val items = OcrScannerEngine.parseOcrTextToItems(text)
                    recognizedItems = items
                    selectedItemIds = items.map { it.id }.toSet()
                    if (items.isEmpty()) {
                        errorMessage = "Text was detected, but no distinct grocery items and quantities were identified. You can edit the text below."
                    }
                } else {
                    errorMessage = "No legible text found in this image. Please ensure good lighting and clear handwriting."
                }
            } catch (e: Exception) {
                errorMessage = "OCR Error: ${e.localizedMessage}"
            } finally {
                isScanning = false
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            processBitmap(bmp)
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                processBitmap(bmp)
            } catch (e: Exception) {
                errorMessage = "Failed to load selected photo"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, GlassCardBorder), RoundedCornerShape(24.dp)),
            color = Color(0xF2FFFFFF) // Translucent frosted glass
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Scan Handwritten List",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Capture a paper customer slip via camera or select from gallery to auto-extract items:",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                // Camera & Gallery Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                            .clickable { cameraLauncher.launch(null) },
                        color = Color(0x99EBF2EE)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryGreen)
                            Spacer(Modifier.width(6.dp))
                            Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                            .clickable { galleryLauncher.launch("image/*") },
                        color = Color(0x99EBF2EE)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryGreen)
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }
                }

                // Image Preview if captured
                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2EBE5))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Slip",
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isScanning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x80000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Reading Handwriting...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Error Message if any
                if (errorMessage != null) {
                    Surface(
                        color = AmberWarningBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = AmberWarning,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Recognized Items List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted Items (${recognizedItems.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (recognizedItems.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedItemIds = if (selectedItemIds.size == recognizedItems.size) emptySet()
                                else recognizedItems.map { it.id }.toSet()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                if (selectedItemIds.size == recognizedItems.size) "Deselect All" else "Select All",
                                fontSize = 11.sp,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                if (recognizedItems.isEmpty() && !isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x40FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Take a photo or pick an image of a grocery slip to extract items automatically",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recognizedItems, key = { it.id }) { itm ->
                            val isSelected = selectedItemIds.contains(itm.id)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xD9FFFFFF) else Color(0x66FFFFFF),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryGreen else Color(0x30FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemIds = if (isSelected) selectedItemIds - itm.id else selectedItemIds + itm.id
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { chk ->
                                                selectedItemIds = if (chk) selectedItemIds + itm.id else selectedItemIds - itm.id
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                                        )
                                        Column {
                                            Text(itm.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("${itm.quantity} ${itm.unit}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }

                                    Text(
                                        text = if (itm.price != null) "₹${itm.price}" else "! Needs Price",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (itm.price != null) PrimaryGreen else AmberWarning
                                    )
                                }
                            }
                        }
                    }
                }

                // Add to Bill Button
                Button(
                    onClick = {
                        val itemsToAdd = recognizedItems.filter { selectedItemIds.contains(it.id) }
                        if (itemsToAdd.isNotEmpty()) {
                            onAddItems(itemsToAdd)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = selectedItemIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Add ${selectedItemIds.size} Items to Bill",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
