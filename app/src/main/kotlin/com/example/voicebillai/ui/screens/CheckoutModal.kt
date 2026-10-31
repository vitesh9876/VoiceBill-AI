package com.example.voicebillai.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.voicebillai.engine.QrCodeGenerator
import com.example.voicebillai.model.Bill
import com.example.voicebillai.model.PaymentMode
import com.example.voicebillai.model.ShopSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckoutModal(
    bill: Bill,
    settings: ShopSettings,
    onDismiss: () -> Unit,
    onFinalize: (PaymentMode) -> Unit
) {
    val context = LocalContext.current
    var customerName by remember { mutableStateOf(bill.customerName) }
    var customerPhone by remember { mutableStateOf(bill.customerPhone) }

    val upiUri = remember(bill.grandTotal, settings.upiVpa) {
        QrCodeGenerator.createUpiUri(
            vpa = settings.upiVpa,
            payeeName = settings.shopName,
            amount = bill.grandTotal,
            transactionNote = "Bill-${bill.id}"
        )
    }

    val qrBitmap = remember(upiUri) {
        QrCodeGenerator.generateQrBitmap(upiUri, size = 400)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = settings.shopName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF176B4D),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${settings.shopAddress} • ${settings.shopPhone}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider()

                // Customer Inputs (for WhatsApp receipt / Khata)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Phone (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Dynamic UPI QR Code Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F6F2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SCAN TO PAY WITH ANY UPI APP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF176B4D)
                        )

                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier
                                    .size(190.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        }

                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.2f", bill.grandTotal)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF20231F)
                        )
                        Text(
                            text = "UPI ID: ${settings.upiVpa}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Itemized Receipt Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INVOICE SUMMARY (#${bill.id})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        bill.items.forEach { itm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${itm.name} (${itm.quantity} ${itm.unit})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", itm.lineAmount ?: 0.0)}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 13.sp)
                            Text("₹${String.format(Locale.getDefault(), "%.2f", bill.subtotal)}", fontSize = 13.sp)
                        }

                        if (bill.discount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount", fontSize = 13.sp, color = Color(0xFF176B4D))
                                Text("-₹${String.format(Locale.getDefault(), "%.2f", bill.discount)}", fontSize = 13.sp, color = Color(0xFF176B4D))
                            }
                        }

                        if (bill.tax > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GST (${bill.taxRate}%)", fontSize = 13.sp)
                                Text("₹${String.format(Locale.getDefault(), "%.2f", bill.tax)}", fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${String.format(Locale.getDefault(), "%.2f", bill.grandTotal)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF176B4D)
                            )
                        }
                    }
                }

                // WhatsApp & Print Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareReceiptViaWhatsApp(context, bill, settings, customerPhone)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            printBillWithAndroidPrintManager(context, bill, settings)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print", fontSize = 12.sp)
                    }
                }

                HorizontalDivider()

                // Finalize Payment Method Buttons
                Text(
                    text = "Confirm Settlement",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onFinalize(PaymentMode.CASH) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20231F))
                    ) {
                        Text("💵 Cash")
                    }

                    Button(
                        onClick = { onFinalize(PaymentMode.UPI) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B4D))
                    ) {
                        Text("📱 UPI Done")
                    }

                    Button(
                        onClick = { onFinalize(PaymentMode.KHATA) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                    ) {
                        Text("📒 Khata")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Edit")
                }
            }
        }
    }
}

private fun formatReceiptText(bill: Bill, settings: ShopSettings): String {
    val dateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date(bill.createdAt))
    val sb = StringBuilder()
    sb.appendLine("🧾 *${settings.shopName}*")
    sb.appendLine("${settings.shopAddress}")
    sb.appendLine("Phone: ${settings.shopPhone}")
    sb.appendLine("Bill #${bill.id} | $dateStr")
    sb.appendLine("--------------------------------")
    bill.items.forEachIndexed { i, itm ->
        sb.appendLine("${i + 1}. ${itm.name} (${itm.quantity} ${itm.unit}) - ₹${String.format(Locale.getDefault(), "%.2f", itm.lineAmount ?: 0.0)}")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("Subtotal: ₹${String.format(Locale.getDefault(), "%.2f", bill.subtotal)}")
    if (bill.discount > 0) sb.appendLine("Discount: -₹${String.format(Locale.getDefault(), "%.2f", bill.discount)}")
    if (bill.tax > 0) sb.appendLine("GST (${bill.taxRate}%): ₹${String.format(Locale.getDefault(), "%.2f", bill.tax)}")
    sb.appendLine("*Grand Total: ₹${String.format(Locale.getDefault(), "%.2f", bill.grandTotal)}*")
    sb.appendLine("--------------------------------")
    sb.appendLine("Pay via UPI: ${settings.upiVpa}")
    sb.appendLine("Thank you for shopping with us! 🙏")
    return sb.toString()
}

private fun shareReceiptViaWhatsApp(context: Context, bill: Bill, settings: ShopSettings, customerPhone: String) {
    val text = formatReceiptText(bill, settings)
    try {
        val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.length >= 10) {
            val fullNumber = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullNumber&text=${Uri.encode(text)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    // Fallback standard share intent
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Receipt"))
}

private fun printBillWithAndroidPrintManager(context: Context, bill: Bill, settings: ShopSettings) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val dateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date(bill.createdAt))
            val itemsRows = bill.items.mapIndexed { idx, itm ->
                val amt = String.format(Locale.getDefault(), "%.2f", itm.lineAmount ?: 0.0)
                """
                <tr>
                    <td style="padding: 4px 0;">${idx + 1}. ${itm.name}</td>
                    <td style="text-align: center; padding: 4px 0;">${itm.quantity} ${itm.unit}</td>
                    <td style="text-align: right; padding: 4px 0;">₹$amt</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")

            val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Invoice #${bill.id}</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        color: #111;
                        padding: 16px;
                        margin: 0;
                        max-width: 320px;
                        font-size: 13px;
                    }
                    .center { text-align: center; }
                    .bold { font-weight: bold; }
                    .shop-name { font-size: 18px; font-weight: bold; color: #176B4D; margin-bottom: 4px; }
                    .dashed-line { border-top: 1px dashed #777; margin: 10px 0; }
                    .flex-row { display: flex; justify-content: space-between; margin: 4px 0; }
                    table { width: 100%; border-collapse: collapse; margin: 8px 0; font-size: 12px; }
                    th { border-bottom: 1px solid #ddd; padding-bottom: 4px; }
                    .total-row { font-size: 16px; font-weight: bold; color: #176B4D; }
                </style>
            </head>
            <body>
                <div class="center shop-name">${settings.shopName}</div>
                <div class="center" style="font-size: 11px; color: #555;">${settings.shopAddress}</div>
                <div class="center" style="font-size: 11px; color: #555;">Phone: ${settings.shopPhone}</div>
                <div class="dashed-line"></div>
                <div class="flex-row"><span>Bill #${bill.id}</span><span>$dateStr</span></div>
                ${if (bill.customerName.isNotBlank()) "<div class=\"flex-row\"><span>Customer: ${bill.customerName}</span><span>${bill.customerPhone}</span></div>" else ""}
                <div class="dashed-line"></div>
                <table>
                    <thead>
                        <tr>
                            <th style="text-align: left;">Item</th>
                            <th style="text-align: center;">Qty</th>
                            <th style="text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsRows
                    </tbody>
                </table>
                <div class="dashed-line"></div>
                <div class="flex-row"><span>Subtotal:</span><span>₹${String.format(Locale.getDefault(), "%.2f", bill.subtotal)}</span></div>
                ${if (bill.discount > 0) "<div class=\"flex-row\" style=\"color:#176B4D;\"><span>Discount:</span><span>-₹${String.format(Locale.getDefault(), "%.2f", bill.discount)}</span></div>" else ""}
                ${if (bill.tax > 0) "<div class=\"flex-row\"><span>GST (${bill.taxRate}%):</span><span>₹${String.format(Locale.getDefault(), "%.2f", bill.tax)}</span></div>" else ""}
                <div class="dashed-line"></div>
                <div class="flex-row total-row"><span>Grand Total:</span><span>₹${String.format(Locale.getDefault(), "%.2f", bill.grandTotal)}</span></div>
                <div class="dashed-line"></div>
                <div class="center" style="font-size: 11px; margin-top: 8px;">Pay via UPI: <b>${settings.upiVpa}</b></div>
                <div class="center bold" style="margin-top: 8px;">Thank You! Visit Again 🙏</div>
            </body>
            </html>
            """.trimIndent()

            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter("Invoice_${bill.id}")
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print("VoiceBill_${bill.id}", printAdapter, printAttributes)
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            return
        }
    } catch (_: Exception) {}

    // Fallback: share receipt text if PrintManager not available
    val text = formatReceiptText(bill, settings)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Print / Share Receipt to Printer"))
}

