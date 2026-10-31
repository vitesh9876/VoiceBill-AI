package com.example.voicebillai.data

import android.content.Context
import android.content.SharedPreferences
import com.example.voicebillai.engine.ArithmeticEngine
import com.example.voicebillai.model.Bill
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.BillStatus
import com.example.voicebillai.model.KhataEntry
import com.example.voicebillai.model.PaymentMode
import com.example.voicebillai.model.ShopSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class VoiceBillRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voicebill_native_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ShopSettings> = _settings.asStateFlow()

    private val _activeBill = MutableStateFlow(loadActiveBill())
    val activeBill: StateFlow<Bill> = _activeBill.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<Bill>> = _history.asStateFlow()

    // Last-Known Price Memory cache
    private val lastKnownPrices = mutableMapOf<String, Double>()

    init {
        loadLastKnownPrices()
    }

    private fun loadSettings(): ShopSettings {
        return ShopSettings(
            shopName = prefs.getString("shop_name", "Sri Lakshmi Kirana & General Store") ?: "Sri Lakshmi Kirana & General Store",
            shopPhone = prefs.getString("shop_phone", "+91 98765 43210") ?: "+91 98765 43210",
            shopAddress = prefs.getString("shop_address", "Shop #4, Main Market, Hyderabad") ?: "Shop #4, Main Market, Hyderabad",
            shopGstin = prefs.getString("shop_gstin", "") ?: "",
            upiVpa = prefs.getString("shop_upi_vpa", "kirana@upi") ?: "kirana@upi",
            speechLang = prefs.getString("speech_lang", "en-IN") ?: "en-IN",
            defaultTaxRate = prefs.getFloat("default_tax_rate", 0.0f).toDouble(),
            enableLastKnownPrice = prefs.getBoolean("enable_last_known_price", true),
            enableSoundboxFeedback = prefs.getBoolean("enable_soundbox_feedback", true)
        )
    }

    fun updateSettings(newSettings: ShopSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("shop_name", newSettings.shopName)
            .putString("shop_phone", newSettings.shopPhone)
            .putString("shop_address", newSettings.shopAddress)
            .putString("shop_gstin", newSettings.shopGstin)
            .putString("shop_upi_vpa", newSettings.upiVpa)
            .putString("speech_lang", newSettings.speechLang)
            .putFloat("default_tax_rate", newSettings.defaultTaxRate.toFloat())
            .putBoolean("enable_last_known_price", newSettings.enableLastKnownPrice)
            .putBoolean("enable_soundbox_feedback", newSettings.enableSoundboxFeedback)
            .apply()
    }

    private fun loadActiveBill(): Bill {
        val b = Bill(
            taxRate = _settings.value.defaultTaxRate,
            items = emptyList()
        )
        return ArithmeticEngine.recalculateBill(b)
    }

    private fun loadHistory(): List<Bill> {
        val list = mutableListOf<Bill>()
        val raw = prefs.getString("bills_history_json", null) ?: return list
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val total = obj.getDouble("grandTotal")
                val customerName = obj.optString("customerName", "Walk-in Customer")
                val customerPhone = obj.optString("customerPhone", "")
                val statusStr = obj.optString("status", "FINALIZED")
                val paymentModeStr = obj.optString("paymentMode", "CASH")
                val date = obj.optLong("createdAt", System.currentTimeMillis())

                list.add(
                    Bill(
                        id = id,
                        createdAt = date,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        grandTotal = total,
                        status = if (statusStr == "FINALIZED") BillStatus.FINALIZED else BillStatus.DRAFT,
                        paymentMode = when (paymentModeStr) {
                            "UPI" -> PaymentMode.UPI
                            "KHATA" -> PaymentMode.KHATA
                            else -> PaymentMode.CASH
                        }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun loadLastKnownPrices() {
        val raw = prefs.getString("last_prices_json", null) ?: return
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                lastKnownPrices[k.lowercase()] = obj.getDouble(k)
            }
        } catch (_: Exception) {}
    }

    private fun saveLastKnownPrices() {
        val obj = JSONObject()
        for ((k, v) in lastKnownPrices) {
            obj.put(k, v)
        }
        prefs.edit().putString("last_prices_json", obj.toString()).apply()
    }

    fun getSuggestedPrice(itemName: String): Double? {
        if (!_settings.value.enableLastKnownPrice) return null
        return lastKnownPrices[itemName.trim().lowercase()]
    }

    fun recordItemPrice(itemName: String, price: Double) {
        if (price > 0.0) {
            lastKnownPrices[itemName.trim().lowercase()] = price
            saveLastKnownPrices()
        }
    }

    fun updateActiveBill(bill: Bill) {
        val recalculated = ArithmeticEngine.recalculateBill(bill)
        _activeBill.value = recalculated
    }

    fun finalizeActiveBill(paymentMode: PaymentMode): Boolean {
        val current = _activeBill.value
        if (!ArithmeticEngine.isReadyToFinalize(current)) return false

        // Record prices in Last-Known Price memory
        for (item in current.items) {
            val p = item.price
            if (p != null) {
                recordItemPrice(item.name, p)
            }
        }

        val finalized = current.copy(
            status = BillStatus.FINALIZED,
            finalizedAt = System.currentTimeMillis(),
            paymentMode = paymentMode
        )

        val updatedHistory = listOf(finalized) + _history.value
        _history.value = updatedHistory
        persistHistory(updatedHistory)

        // Reset to new fresh bill
        startNewBill()
        return true
    }

    fun startNewBill() {
        _activeBill.value = Bill(
            taxRate = _settings.value.defaultTaxRate,
            items = emptyList()
        )
    }

    private fun persistHistory(list: List<Bill>) {
        val arr = JSONArray()
        for (b in list.take(100)) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("createdAt", b.createdAt)
            obj.put("customerName", b.customerName)
            obj.put("customerPhone", b.customerPhone)
            obj.put("grandTotal", b.grandTotal)
            obj.put("status", b.status.name)
            obj.put("paymentMode", b.paymentMode.name)
            arr.put(obj)
        }
        prefs.edit().putString("bills_history_json", arr.toString()).apply()
    }

    fun getKhataLedger(): List<KhataEntry> {
        val khataBills = _history.value.filter { it.paymentMode == PaymentMode.KHATA }
        val map = khataBills.groupBy { (it.customerName.ifBlank { "Unregistered" }) to it.customerPhone }
        return map.map { (pair, bills) ->
            KhataEntry(
                customerName = pair.first,
                customerPhone = pair.second,
                totalCredit = bills.sumOf { it.grandTotal },
                bills = bills
            )
        }.sortedByDescending { it.totalCredit }
    }
}

