package com.example.voicebillai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicebillai.engine.ArithmeticEngine
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.DiscountType
import com.example.voicebillai.ui.AppTab
import com.example.voicebillai.ui.SpeechState
import com.example.voicebillai.ui.VoiceBillViewModel
import com.example.voicebillai.ui.components.GlassBackground
import com.example.voicebillai.ui.components.GlassCard
import com.example.voicebillai.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: VoiceBillViewModel,
    onNavigateToCheckout: () -> Unit
) {
    val bill by viewModel.activeBill.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val feedbackMsg by viewModel.userFeedbackMessage.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var showOcrModal by remember { mutableStateOf(false) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showStoreRenameModal by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BillItem?>(null) }
    var discountUnitDropdown by remember { mutableStateOf(false) }
    var gstDropdownExpanded by remember { mutableStateOf(false) }

    val isReady = remember(bill) { ArithmeticEngine.isReadyToFinalize(bill) }
    val missingPricesCount = remember(bill) { bill.items.count { it.price == null || it.isMissingPrice } }

    val languages = listOf(
        "en-IN" to "English (India)",
        "te-IN" to "Telugu (తెలుగు)",
        "hi-IN" to "Hindi (हिन्दी)",
        "ta-IN" to "Tamil (தமிழ்)",
        "kn-IN" to "Kannada (ಕನ್ನಡ)"
    )

    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header: Logo, App Title, Subtitle, Scan & Settings Icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // App Logo with gradient and voice wave
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF229263), Color(0xFF135F3E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "≋",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "VoiceBill AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Voice-First Billing",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Top Right Frosted Buttons: Scan & Settings
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrostedIconButton(
                            icon = Icons.Outlined.CropFree,
                            contentDescription = "Scan",
                            onClick = { showOcrModal = true }
                        )
                        FrostedIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            onClick = { viewModel.setTab(AppTab.SETTINGS) }
                        )
                    }
                }
            }

            // 2. Store & Language Selector Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Store Pill (left)
                    Surface(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                            .clickable { showStoreRenameModal = true },
                        color = GlassCardBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TextPrimary
                                )
                                Text(
                                    text = settings.shopName.ifBlank { "Sri Sai Balaji Stores" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                        }
                    }

                    // Language Pill (right)
                    Surface(
                        modifier = Modifier
                            .weight(0.9f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                            .clickable { showLanguageModal = true },
                        color = GlassCardBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = TextPrimary
                                )
                                val currentLangLabel = when (settings.speechLang) {
                                    "te-IN" -> "Telugu"
                                    "hi-IN" -> "Hindi"
                                    "ta-IN" -> "Tamil"
                                    "kn-IN" -> "Kannada"
                                    "ml-IN" -> "Malayalam"
                                    "mr-IN" -> "Marathi"
                                    "bn-IN" -> "Bengali"
                                    "gu-IN" -> "Gujarati"
                                    "pa-IN" -> "Punjabi"
                                    else -> "English"
                                }
                                Text(
                                    text = currentLangLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Hero "New Bill" Card with Mic & Soundwaves
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title & Subtitle
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "New Bill",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Speak items, scan handwritten list or add manually",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }

                        // Soundwaves + Circular Glowing Mic
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Soundwave Bars
                            SoundwaveVisualizer(isListening = speechState.isListening)

                            Spacer(Modifier.width(16.dp))

                            // Glowing Mic Button
                            HeroMicButton(
                                isListening = speechState.isListening,
                                onToggle = {
                                    if (speechState.isListening) viewModel.stopListening()
                                    else viewModel.startListening()
                                }
                            )

                            Spacer(Modifier.width(16.dp))

                            // Right Soundwave Bars
                            SoundwaveVisualizer(isListening = speechState.isListening)
                        }

                        // Guidance Texts
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (speechState.isListening) "Listening... speak now" else "Tap & speak your items",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (speechState.isListening) Color(0xFFE53935) else TextPrimary
                            )
                            Text(
                                text = "e.g., \"Rice 1 kg, Sugar 500 g, Milk 2 packets\"",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Live Speech Transcript
                        if (speechState.transcript.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x66FFFFFF),
                                border = BorderStroke(1.dp, Color(0x66A8D5C2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${speechState.transcript}\"",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreenDark,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 4. Split Action Buttons: [ ] Scan List | (+) Add Item
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Scan List Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(14.dp))
                            .clickable { showOcrModal = true },
                        color = GlassCardBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CropFree,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Scan List",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Add Item Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(14.dp))
                            .clickable { showManualAddDialog = true },
                        color = GlassCardBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AddBox,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Add Item",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // 5. Bill Items Card (Empty State or List + Discount/GST + Grand Total + Review Bill)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Header: Cart Icon + Bill Items (count) + Clear button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = TextPrimary
                                )
                                Text(
                                    text = "Bill Items (${bill.items.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            if (bill.items.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, GlassCardBorder, RoundedCornerShape(14.dp))
                                        .clickable { viewModel.clearBill() },
                                    color = Color(0x66FFFFFF)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Items or Recessed Empty Box
                        if (bill.items.isEmpty()) {
                            // Recessed "Your bill is empty" box matching the screenshot
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(18.dp)),
                                color = Color(0x55FFFFFF)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Recessed circular cart icon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77FFFFFF))
                                            .border(1.dp, Color(0x55FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.ShoppingCart,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Text(
                                        text = "Your bill is empty",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = "Speak your items or add them manually",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            // Items list rows with prices, missing-price badge, and 1-tap price suggestion
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                bill.items.forEach { item ->
                                    BillItemRow(
                                        item = item,
                                        enableLastKnownPrice = settings.enableLastKnownPrice,
                                        onApplySuggestedPrice = { viewModel.applySuggestedPrice(item) },
                                        onPriceEntered = { p -> viewModel.setItemPrice(item.id, p) },
                                        onEdit = { editingItem = item },
                                        onDelete = { viewModel.removeItem(item.id) }
                                    )
                                }
                            }
                        }

                        // Discount & GST / Tax Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left: Discount
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.LocalOffer,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = TextSecondary
                                    )
                                    Text("Discount", fontSize = 11.sp, color = TextSecondary)
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp)),
                                    color = Color(0x66FFFFFF)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        var discountInput by remember(bill.discountValue) {
                                            mutableStateOf(if (bill.discountValue > 0) bill.discountValue.toString() else "0")
                                        }
                                        BasicGlassTextField(
                                            value = discountInput,
                                            onValueChange = {
                                                discountInput = it
                                                val v = it.toDoubleOrNull() ?: 0.0
                                                viewModel.setDiscount(bill.discountType, v)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .clickable { discountUnitDropdown = true }
                                                    .padding(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (bill.discountType == DiscountType.PERCENT) "%" else "₹",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = TextSecondary
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = discountUnitDropdown,
                                                onDismissRequest = { discountUnitDropdown = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("% (Percentage)") },
                                                    onClick = {
                                                        viewModel.setDiscount(DiscountType.PERCENT, bill.discountValue)
                                                        discountUnitDropdown = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("₹ (Flat Amount)") },
                                                    onClick = {
                                                        viewModel.setDiscount(DiscountType.AMOUNT, bill.discountValue)
                                                        discountUnitDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Right: GST / Tax
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = TextSecondary
                                    )
                                    Text("GST / Tax", fontSize = 11.sp, color = TextSecondary)
                                }

                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                                            .clickable { gstDropdownExpanded = true },
                                        color = Color(0x66FFFFFF)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val taxLabel = when (bill.taxRate) {
                                                0.0 -> "0% (Exempt)"
                                                5.0 -> "5% GST"
                                                12.0 -> "12% GST"
                                                18.0 -> "18% GST"
                                                else -> "${bill.taxRate}%"
                                            }
                                            Text(taxLabel, fontSize = 12.sp, color = TextPrimary)
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = TextSecondary
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = gstDropdownExpanded,
                                        onDismissRequest = { gstDropdownExpanded = false }
                                    ) {
                                        listOf(0.0 to "0% (Exempt)", 5.0 to "5% GST", 12.0 to "12% GST", 18.0 to "18% GST").forEach { (rate, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, fontSize = 12.sp) },
                                                onClick = {
                                                    viewModel.setTaxRate(rate)
                                                    gstDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0x40D8E2DC),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Grand Total Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%.2f", bill.grandTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen
                            )
                        }

                        // Big Gradient Action Button: Review Bill →
                        Button(
                            onClick = onNavigateToCheckout,
                            enabled = isReady,
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFD4DFD8)
                            ),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isReady) {
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF229263), Color(0xFF186F4E), Color(0xFF12563C))
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFBDCCC2), Color(0xFFBDCCC2))
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (missingPricesCount > 0) "$missingPricesCount Items Need Price" else "Review Bill →",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualAddDialog) {
        ManualItemDialog(
            item = null,
            onDismiss = { showManualAddDialog = false },
            onSave = {
                viewModel.saveItem(it)
                showManualAddDialog = false
            }
        )
    }

    if (showOcrModal) {
        OcrScanModal(
            onDismiss = { showOcrModal = false },
            onAddItems = { items ->
                items.forEach { viewModel.saveItem(it) }
            }
        )
    }

    if (showLanguageModal) {
        LanguageSelectionModal(
            currentLang = settings.speechLang,
            onDismiss = { showLanguageModal = false },
            onSelect = { selectedCode ->
                viewModel.updateSettings(settings.copy(speechLang = selectedCode))
                showLanguageModal = false
            }
        )
    }

    if (showStoreRenameModal) {
        StoreRenameModal(
            currentName = settings.shopName,
            onDismiss = { showStoreRenameModal = false },
            onSave = { newName ->
                viewModel.updateSettings(settings.copy(shopName = newName))
                showStoreRenameModal = false
            }
        )
    }

    if (editingItem != null) {
        ManualItemDialog(
            item = editingItem,
            onDismiss = { editingItem = null },
            onSave = {
                viewModel.saveItem(it)
                editingItem = null
            }
        )
    }
}

/**
 * Top bar frosted icon button
 */
@Composable
fun FrostedIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = GlassCardBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
    }
}

/**
 * Hero center microphone button with glowing aura
 */
@Composable
fun HeroMicButton(
    isListening: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(86.dp)
    ) {
        // Glowing frosted outer halo ring
        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(auraScale)
                .clip(CircleShape)
                .background(
                    if (isListening) Color(0x33E53935)
                    else Color(0x33229263)
                )
                .border(
                    1.5.dp,
                    if (isListening) Color(0x55E53935) else Color(0x44229263),
                    CircleShape
                )
        )

        // Center emerald button
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(
                    if (isListening) {
                        Brush.radialGradient(listOf(Color(0xFFEF5350), Color(0xFFC62828)))
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF4AC28E), Color(0xFF229263), Color(0xFF136442)))
                    }
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/**
 * 6-bar Soundwave visualizer matching screenshot
 */
@Composable
fun SoundwaveVisualizer(isListening: Boolean) {
    val heights = listOf(10.dp, 18.dp, 26.dp, 20.dp, 14.dp, 8.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SoundwaveMint)
            )
        }
    }
}

/**
 * Item row in active bill with missing price badge & 1-tap price suggestion
 */
@Composable
fun BillItemRow(
    item: BillItem,
    enableLastKnownPrice: Boolean,
    onApplySuggestedPrice: () -> Unit,
    onPriceEntered: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp)),
        color = Color(0x66FFFFFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = Color(0x66E2E8E4),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = TextMuted)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isMissingPrice || item.price == null) {
                    Surface(
                        color = AmberWarningBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "! Needs Price",
                            color = AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    var pInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.width(76.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 6.dp)) {
                                BasicGlassTextField(
                                    value = pInput,
                                    onValueChange = { pInput = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pInput.isEmpty()) Text("₹ price", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        IconButton(
                            onClick = {
                                val p = pInput.toDoubleOrNull()
                                if (p != null && p > 0) onPriceEntered(p)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Text(
                        text = "@ ₹${String.format(Locale.getDefault(), "%.2f", item.price)} / ${item.priceUnit}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", item.lineAmount ?: 0.0)}",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            // 1-Tap "Last-Known Price" Suggestion Chip
            if (enableLastKnownPrice && item.isMissingPrice && item.suggestedPrice != null) {
                Surface(
                    color = Color(0xCCECFDF5),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable(onClick = onApplySuggestedPrice)
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Last sold at ₹${String.format(Locale.getDefault(), "%.2f", item.suggestedPrice)}",
                            fontSize = 11.sp,
                            color = Color(0xFF065F46)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "[Use ₹${item.suggestedPrice.toInt()}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/**
 * Frosted Glass Language Selection Modal with Native Scripts
 */
@Composable
fun LanguageSelectionModal(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        Triple("en-IN", "English (India)", "Indian English Speech"),
        Triple("te-IN", "Telugu", "తెలుగు (కిరాణా & వస్తువులు)"),
        Triple("hi-IN", "Hindi", "हिन्दी (दुकान व सामग्री)"),
        Triple("ta-IN", "Tamil", "தமிழ் (மளிகை பொருட்கள்)"),
        Triple("kn-IN", "Kannada", "ಕನ್ನಡ (ದಿನಸಿ ಸಾಮಗ್ರಿಗಳು)"),
        Triple("ml-IN", "Malayalam", "മലയാളം (പലചരക്ക് സാധനങ്ങൾ)"),
        Triple("mr-IN", "Marathi", "मराठी (किराणा व माल)"),
        Triple("bn-IN", "Bengali", "বাংলা (মুদি সামগ্রী)"),
        Triple("gu-IN", "Gujarati", "ગુજરાતી (કરિયાણું)"),
        Triple("pa-IN", "Punjabi", "ਪੰਜਾਬੀ (ਕਿਰਾਨਾ)")
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, GlassCardBorder), RoundedCornerShape(24.dp)),
            color = Color(0xF7FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Speech Language",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Select speech recognition language for voice billing and kirana soundbox:",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(languages) { (code, englishName, nativeName) ->
                        val isSelected = (code == currentLang)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isSelected) PrimaryGreen else Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { onSelect(code) },
                            color = if (isSelected) Color(0xD9EBF5F0) else Color(0x66FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = englishName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryGreen else TextPrimary
                                    )
                                    Text(
                                        text = nativeName,
                                        fontSize = 11.5.sp,
                                        color = if (isSelected) PrimaryGreenDark else TextSecondary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Frosted Glass Store Rename Dialog
 */
@Composable
fun StoreRenameModal(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var storeName by remember { mutableStateOf(currentName) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .border(BorderStroke(1.dp, GlassCardBorder), RoundedCornerShape(22.dp)),
            color = Color(0xF5FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Store Name",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x66FFFFFF),
                        unfocusedContainerColor = Color(0x66FFFFFF),
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (storeName.isNotBlank()) onSave(storeName.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
