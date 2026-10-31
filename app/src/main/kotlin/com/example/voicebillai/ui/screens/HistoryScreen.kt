package com.example.voicebillai.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicebillai.model.Bill
import com.example.voicebillai.model.PaymentMode
import com.example.voicebillai.ui.VoiceBillViewModel
import com.example.voicebillai.ui.components.GlassBackground
import com.example.voicebillai.ui.components.GlassCard
import com.example.voicebillai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: VoiceBillViewModel) {
    val history by viewModel.history.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.id.contains(searchQuery, ignoreCase = true) ||
                    it.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.customerPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalRevenue = remember(history) { history.sumOf { it.grandTotal } }
    val cashTotal = remember(history) { history.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.grandTotal } }
    val upiTotal = remember(history) { history.filter { it.paymentMode == PaymentMode.UPI }.sumOf { it.grandTotal } }
    val khataTotal = remember(history) { history.filter { it.paymentMode == PaymentMode.KHATA }.sumOf { it.grandTotal } }

    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Bill History & Register",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${history.size} finalized customer invoices",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Frosted EOD Register Summary ("Hisaab-Kitaab")
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📊 Today's Register Summary",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Sales", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalRevenue)}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Invoices", fontSize = 11.sp, color = TextSecondary)
                                Text("${history.size}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            }
                        }

                        HorizontalDivider(color = Color(0x30FFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💵 Cash: ₹${String.format(Locale.getDefault(), "%.0f", cashTotal)}", fontSize = 12.sp, color = TextPrimary)
                            Text("📱 UPI: ₹${String.format(Locale.getDefault(), "%.0f", upiTotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                            Text("📒 Khata: ₹${String.format(Locale.getDefault(), "%.0f", khataTotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AmberWarning)
                        }
                    }
                }
            }

            // Frosted Search Bar
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, GlassCardBorder, RoundedCornerShape(14.dp)),
                    color = GlassCardBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TextPrimary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) Text("Search by Bill # or Customer...", fontSize = 13.sp, color = TextMuted)
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // List of Bills
            if (filteredHistory.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(44.dp), tint = TextMuted)
                            Text("No Past Invoices Found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Finalized bills will automatically appear in this register", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredHistory, key = { it.id }) { b ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = b.id,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = when (b.paymentMode) {
                                            PaymentMode.UPI -> Color(0xFFD1E7DD)
                                            PaymentMode.KHATA -> Color(0xFFFEF3C7)
                                            PaymentMode.CASH -> Color(0xFFE5E7EB)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = b.paymentMode.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (b.paymentMode) {
                                                PaymentMode.UPI -> Color(0xFF0F5132)
                                                PaymentMode.KHATA -> Color(0xFF92400E)
                                                PaymentMode.CASH -> Color(0xFF374151)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(b.createdAt))
                                Text(
                                    text = if (b.customerName.isNotBlank()) "${b.customerName} • $dateStr" else dateStr,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", b.grandTotal)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryGreen
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val text = "🧾 Bill #${b.id} | Amount: ₹${String.format(Locale.getDefault(), "%.2f", b.grandTotal)} | Payment: ${b.paymentMode.name}\n${settings.shopName}"
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, text)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Bill"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp), tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
