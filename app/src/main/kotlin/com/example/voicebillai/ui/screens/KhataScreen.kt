package com.example.voicebillai.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicebillai.model.KhataEntry
import com.example.voicebillai.ui.VoiceBillViewModel
import com.example.voicebillai.ui.components.GlassBackground
import com.example.voicebillai.ui.components.GlassCard
import com.example.voicebillai.ui.theme.*
import java.util.Locale

@Composable
fun KhataScreen(viewModel: VoiceBillViewModel) {
    val ledger = remember(viewModel.history.collectAsState().value) {
        viewModel.getKhataLedger()
    }
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    val totalUdhar = remember(ledger) {
        ledger.sumOf { it.totalCredit }
    }

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
                        text = "Digital Khata (Customer Credit)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Track customer udhar & send payment reminders",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Total Outstanding Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TOTAL OUTSTANDING BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )

                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.2f", totalUdhar)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberWarning
                        )

                        Text(
                            text = "Across ${ledger.size} customer accounts",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Customer Credit List
            if (ledger.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(44.dp), tint = TextMuted)
                            Text("No Pending Khata Entries", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("When finalizing a bill, select 'Khata' to record customer credit.", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(ledger, key = { it.customerName + it.customerPhone }) { entry ->
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
                                Text(
                                    text = entry.customerName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (entry.customerPhone.isNotBlank()) {
                                    Text(
                                        text = "📞 ${entry.customerPhone}",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "${entry.bills.size} unpaid credit bills",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", entry.totalCredit)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberWarning
                                )

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0x66B45309), RoundedCornerShape(12.dp))
                                        .clickable {
                                            val reminder = "Namaste ${entry.customerName}, your total pending balance at *${settings.shopName}* is *₹${String.format(Locale.getDefault(), "%.2f", entry.totalCredit)}*. Kindly pay via UPI to: ${settings.upiVpa}. Dhanyavad!"
                                            val cleanPhone = entry.customerPhone.replace(Regex("[^0-9]"), "")
                                            if (cleanPhone.length >= 10) {
                                                val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=${Uri.encode(reminder)}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            } else {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, reminder)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Send Payment Reminder"))
                                            }
                                        },
                                    color = Color(0x40FEF3C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(12.dp), tint = AmberWarning)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
