package com.example.voicebillai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicebillai.model.ShopSettings
import com.example.voicebillai.ui.VoiceBillViewModel
import com.example.voicebillai.ui.components.GlassBackground
import com.example.voicebillai.ui.components.GlassCard
import com.example.voicebillai.ui.theme.*

@Composable
fun SettingsScreen(viewModel: VoiceBillViewModel) {
    val currentSettings by viewModel.settings.collectAsState()

    var shopName by remember(currentSettings) { mutableStateOf(currentSettings.shopName) }
    var shopPhone by remember(currentSettings) { mutableStateOf(currentSettings.shopPhone) }
    var shopAddress by remember(currentSettings) { mutableStateOf(currentSettings.shopAddress) }
    var shopGstin by remember(currentSettings) { mutableStateOf(currentSettings.shopGstin) }
    var upiVpa by remember(currentSettings) { mutableStateOf(currentSettings.upiVpa) }
    var speechLang by remember(currentSettings) { mutableStateOf(currentSettings.speechLang) }
    var enableLastKnownPrice by remember(currentSettings) { mutableStateOf(currentSettings.enableLastKnownPrice) }
    var enableSoundboxFeedback by remember(currentSettings) { mutableStateOf(currentSettings.enableSoundboxFeedback) }
    var langDropdownExpanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "en-IN" to "English (India)",
        "te-IN" to "Telugu (తెలుగు)",
        "hi-IN" to "Hindi (हिन्दी)",
        "ta-IN" to "Tamil (தமிழ்)",
        "kn-IN" to "Kannada (ಕನ್ನಡ)",
        "ml-IN" to "Malayalam (മലയാളം)",
        "mr-IN" to "Marathi (मराठी)",
        "bn-IN" to "Bengali (বাংলা)",
        "gu-IN" to "Gujarati (ગુજરાતી)",
        "pa-IN" to "Punjabi (ਪੰਜਾਬੀ)"
    )

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Store Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure UPI VPA, Audio Soundbox, & Price Memory",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Section 1: Feature Toggles
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "⚡ Intelligent Feature Toggles",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    // Toggle 1: Last-Known Price Memory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Last-Known Price Memory",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Suggests past rates when you dictate items without a price (Zero-Inventory with memory)",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableLastKnownPrice,
                            onCheckedChange = { enableLastKnownPrice = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0x30FFFFFF))

                    // Toggle 2: Kirana Soundbox Audio Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kirana Soundbox Audio Feedback",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Speaks added items, line amounts, and payment totals out loud like a retail Soundbox",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableSoundboxFeedback,
                            onCheckedChange = { enableSoundboxFeedback = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen
                            )
                        )
                    }
                }
            }

            // Section 2: Store Identity & UPI VPA
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏪 Store Identity & UPI Settlement",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    FrostedInputField(
                        label = "Store Name",
                        value = shopName,
                        onValueChange = { shopName = it }
                    )

                    FrostedInputField(
                        label = "Shopkeeper UPI ID / VPA *",
                        value = upiVpa,
                        placeholder = "e.g. kirana@upi or 9876543210@paytm",
                        onValueChange = { upiVpa = it }
                    )

                    FrostedInputField(
                        label = "Store Mobile Number",
                        value = shopPhone,
                        onValueChange = { shopPhone = it }
                    )

                    FrostedInputField(
                        label = "Store Address",
                        value = shopAddress,
                        onValueChange = { shopAddress = it }
                    )

                    FrostedInputField(
                        label = "GSTIN (Optional)",
                        value = shopGstin,
                        onValueChange = { shopGstin = it }
                    )
                }
            }

            // Section 3: Speech Language Dropdown
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🗣️ Voice Recognition Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                                .clickable { langDropdownExpanded = true },
                            color = Color(0x66FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = languages.find { it.first == speechLang }?.second ?: speechLang,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = langDropdownExpanded,
                            onDismissRequest = { langDropdownExpanded = false }
                        ) {
                            languages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 13.sp) },
                                    onClick = {
                                        speechLang = code
                                        langDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    val updated = currentSettings.copy(
                        shopName = shopName.trim(),
                        shopPhone = shopPhone.trim(),
                        shopAddress = shopAddress.trim(),
                        shopGstin = shopGstin.trim(),
                        upiVpa = upiVpa.trim(),
                        speechLang = speechLang,
                        enableLastKnownPrice = enableLastKnownPrice,
                        enableSoundboxFeedback = enableSoundboxFeedback
                    )
                    viewModel.updateSettings(updated)
                    viewModel.postMessage("Settings saved successfully!")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF229263), Color(0xFF186F4E), Color(0xFF12563C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FrostedInputField(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotBlank()) { { Text(placeholder, fontSize = 12.sp, color = TextMuted) } } else null,
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
    }
}
