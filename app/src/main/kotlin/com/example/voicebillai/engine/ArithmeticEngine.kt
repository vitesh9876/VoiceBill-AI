package com.example.voicebillai.engine

import com.example.voicebillai.model.Bill
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.DiscountType
import com.example.voicebillai.model.PriceBasis
import java.math.BigDecimal
import java.math.RoundingMode

object ArithmeticEngine {

    fun normalizeUnit(rawUnit: String?): String {
        if (rawUnit.isNullOrBlank()) return "pcs"
        val clean = rawUnit.trim().lowercase().replace(".", "").replace(" ", "")
        return when {
            clean in listOf("kg", "kilo", "kilos", "kilogram", "kilograms", "కిలో", "కిలోలు", "కిలోల", "किलो", "किलोग्राम") -> "kg"
            clean in listOf("g", "gm", "gms", "gram", "grams", "గ్రాం", "గ్రాములు", "గ్రాముల", "గ్రాము", "ग्राम") -> "g"
            clean in listOf("l", "lt", "ltr", "litre", "litres", "liter", "liters", "లీటరు", "లీటర్లు", "లీటర్ల", "लीटर") -> "L"
            clean in listOf("ml", "milli", "millilitre", "millilitres", "మిల్లీ", "मिली") -> "ml"
            clean in listOf("pkt", "packet", "packets", "pack", "packs", "ప్యాకెట్", "ప్యాకెట్లు", "ప్యాకెట్ల", "पैकेट") -> "pkt"
            clean in listOf("bag", "bags", "katta", "కట్ట", "బోరి", "बोरी") -> "bag"
            clean in listOf("box", "boxes", "పెట్టె", "డిబ్బా", "डिब्बा") -> "box"
            clean in listOf("bottle", "bottles", "సీసా", "बोतल") -> "bottle"
            else -> "pcs"
        }
    }

    /**
     * Calculates line amount deterministically using BigDecimal.
     * Prevents IEEE 754 floating point rounding errors.
     */
    fun calculateLineAmount(item: BillItem): Double? {
        val price = item.price ?: return null
        if (price < 0.0) return null
        val qty = if (item.quantity > 0.0) item.quantity else 1.0

        val priceDec = BigDecimal.valueOf(price)
        val qtyDec = BigDecimal.valueOf(qty)
        val normUnit = normalizeUnit(item.unit)
        val normPriceUnit = normalizeUnit(item.priceUnit)

        val amountDec: BigDecimal = when {
            // e.g. 500 g at ₹60/kg -> (500 / 1000) * 60 = ₹30.00
            normUnit == "g" && (normPriceUnit == "kg" || item.priceBasis == PriceBasis.UNIT_RATE) -> {
                qtyDec.divide(BigDecimal.valueOf(1000.0), 6, RoundingMode.HALF_UP).multiply(priceDec)
            }
            // e.g. 250 ml at ₹160/L -> (250 / 1000) * 160 = ₹40.00
            normUnit == "ml" && (normPriceUnit == "L" || item.priceBasis == PriceBasis.UNIT_RATE) -> {
                qtyDec.divide(BigDecimal.valueOf(1000.0), 6, RoundingMode.HALF_UP).multiply(priceDec)
            }
            // Pack price e.g. 500ml bottle for ₹180 (packCount = 1) -> ₹180
            item.priceBasis == PriceBasis.PACK_PRICE -> {
                val packCount = if (item.packCount > 0.0) item.packCount else 1.0
                BigDecimal.valueOf(packCount).multiply(priceDec)
            }
            // Direct count or unit rate e.g. 3 pcs at ₹35/pc -> 105
            else -> {
                qtyDec.multiply(priceDec)
            }
        }

        return amountDec.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun recalculateBill(bill: Bill): Bill {
        val updatedItems = bill.items.map { it.copy(lineAmount = calculateLineAmount(it)) }

        var subtotalDec = BigDecimal.ZERO
        for (it in updatedItems) {
            val amt = it.lineAmount
            if (amt != null) {
                subtotalDec = subtotalDec.add(BigDecimal.valueOf(amt))
            }
        }
        subtotalDec = subtotalDec.setScale(2, RoundingMode.HALF_UP)

        // Discount
        val discountDec: BigDecimal = when (bill.discountType) {
            DiscountType.PERCENT -> {
                val rate = BigDecimal.valueOf(bill.discountValue.coerceIn(0.0, 100.0))
                subtotalDec.multiply(rate).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP)
            }
            DiscountType.AMOUNT -> {
                BigDecimal.valueOf(bill.discountValue.coerceAtLeast(0.0)).min(subtotalDec).setScale(2, RoundingMode.HALF_UP)
            }
        }

        val taxable = subtotalDec.subtract(discountDec).max(BigDecimal.ZERO)

        // Tax
        val taxRateDec = BigDecimal.valueOf(bill.taxRate.coerceAtLeast(0.0))
        val taxDec = taxable.multiply(taxRateDec).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP)

        // Grand Total
        val grandTotalDec = taxable.add(taxDec).setScale(2, RoundingMode.HALF_UP)

        return bill.copy(
            items = updatedItems,
            subtotal = subtotalDec.toDouble(),
            discount = discountDec.toDouble(),
            tax = taxDec.toDouble(),
            grandTotal = grandTotalDec.toDouble()
        )
    }

    fun isReadyToFinalize(bill: Bill): Boolean {
        if (bill.items.isEmpty()) return false
        return bill.items.none { it.price == null || it.isMissingPrice }
    }
}

