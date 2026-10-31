package com.example.voicebillai.model

import java.util.UUID

enum class PriceBasis {
    UNIT_RATE,   // e.g. ₹60/kg, ₹160/L
    PACK_PRICE   // e.g. ₹180 for 500ml bottle, ₹35 for 1 soap packet
}

enum class DiscountType {
    AMOUNT,
    PERCENT
}

enum class PaymentMode {
    CASH,
    UPI,
    KHATA
}

enum class BillStatus {
    DRAFT,
    FINALIZED
}

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val price: Double? = null,
    val priceUnit: String = "pcs",
    val priceBasis: PriceBasis = PriceBasis.PACK_PRICE,
    val packCount: Double = 1.0,
    val lineAmount: Double? = null,
    val isMissingPrice: Boolean = (price == null),
    val isMissingQuantity: Boolean = false,
    val inputSource: String = "voice",
    val suggestedPrice: Double? = null // For "Last-known price" suggestion
)

data class Bill(
    val id: String = "VB-" + System.currentTimeMillis().toString(36).uppercase(),
    val createdAt: Long = System.currentTimeMillis(),
    val finalizedAt: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<BillItem> = emptyList(),
    val discountType: DiscountType = DiscountType.AMOUNT,
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val status: BillStatus = BillStatus.DRAFT
)

data class ShopSettings(
    val shopName: String = "Sri Lakshmi Kirana & General Store",
    val shopPhone: String = "+91 98765 43210",
    val shopAddress: String = "Shop #4, Main Market, Hyderabad",
    val shopGstin: String = "",
    val upiVpa: String = "kirana@upi",
    val speechLang: String = "en-IN",
    val defaultTaxRate: Double = 0.0,
    // Toggles requested by user:
    val enableLastKnownPrice: Boolean = true,
    val enableSoundboxFeedback: Boolean = true
)

data class KhataEntry(
    val customerName: String,
    val customerPhone: String,
    val totalCredit: Double,
    val bills: List<Bill>
)

