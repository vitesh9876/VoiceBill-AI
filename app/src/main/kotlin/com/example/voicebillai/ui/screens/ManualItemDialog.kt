package com.example.voicebillai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.voicebillai.engine.ArithmeticEngine
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.PriceBasis
import com.example.voicebillai.ui.theme.*
import java.util.UUID

@Composable
fun ManualItemDialog(
    item: BillItem?,
    onDismiss: () -> Unit,
    onSave: (BillItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var quantityStr by remember { mutableStateOf(item?.quantity?.toString() ?: "1.0") }
    var unit by remember { mutableStateOf(item?.unit ?: "pcs") }
    var priceStr by remember { mutableStateOf(item?.price?.toString().orEmpty()) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val units = listOf("kg", "g", "L", "ml", "pcs", "pkt", "bag", "box", "bottle")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, GlassCardBorder), RoundedCornerShape(24.dp)),
            color = Color(0xF5FFFFFF) // Translucent frosted glass
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item == null) "Add Product" else "Edit Product",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Item Name
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Product Name *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Rice, Sugar, Sunflower Oil", fontSize = 13.sp) },
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

                // Quantity & Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quantity
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quantity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x66FFFFFF),
                                unfocusedContainerColor = Color(0x66FFFFFF),
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = GlassCardBorder
                            )
                        )
                    }

                    // Unit
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Unit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Box {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { unitDropdownExpanded = true },
                                color = Color(0x66FFFFFF)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(unit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                                }
                            }

                            DropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, fontSize = 13.sp) },
                                        onClick = {
                                            unit = u
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Price
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Unit / Pack Price (₹) [Leave blank if unknown]", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        placeholder = { Text("e.g. 60 (Optional)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x66FFFFFF),
                            unfocusedContainerColor = Color(0x66FFFFFF),
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = GlassCardBorder
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Save Action Button
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val qty = quantityStr.toDoubleOrNull() ?: 1.0
                            val pr = priceStr.toDoubleOrNull()
                            val normU = ArithmeticEngine.normalizeUnit(unit)
                            val newItem = BillItem(
                                id = item?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                quantity = qty,
                                unit = normU,
                                price = pr,
                                priceUnit = if (normU == "g") "kg" else if (normU == "ml") "L" else normU,
                                priceBasis = if (normU in listOf("bottle", "pkt", "pcs", "box")) PriceBasis.PACK_PRICE else PriceBasis.UNIT_RATE,
                                isMissingPrice = (pr == null),
                                inputSource = "manual"
                            )
                            onSave(newItem)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    enabled = name.isNotBlank()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF229263), Color(0xFF186F4E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Item", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
