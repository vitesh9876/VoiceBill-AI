package com.example.voicebillai.engine

import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.PriceBasis
import java.util.UUID

sealed class VoiceActionResult {
    data class ClearAll(val command: String) : VoiceActionResult()
    data class RemoveItem(val itemName: String) : VoiceActionResult()
    object ReviewBill : VoiceActionResult()
    object PrintBill : VoiceActionResult()
    object NewBill : VoiceActionResult()
    data class PriceUpdate(val targetName: String?, val price: Double) : VoiceActionResult()
    data class QuantityUpdate(val quantity: Double, val unit: String) : VoiceActionResult()
    data class AddItems(val items: List<BillItem>) : VoiceActionResult()
    object None : VoiceActionResult()
}

object VoiceParserEngine {

    private val CLEAR_PATTERNS = listOf(
        Regex("^(?:please\\s+)?(?:remove|clear|delete|empty|reset|clean|cancel)\\s+(?:all(?:\\s+items)?|everything|the\\s+bill|bill|all\\s+rows)$", RegexOption.IGNORE_CASE),
        Regex("^(?:remove\\s+all|clear\\s+all|delete\\s+all|clear\\s+bill|empty\\s+bill)$", RegexOption.IGNORE_CASE),
        Regex("^(?:అన్నీ\\s*తొలగించు|అన్నీ\\s*తీసివేయి|మొత్తం\\s*తొలగించు|బిల్లు\\s*ఖాళీ\\s*చేయి)$", RegexOption.IGNORE_CASE),
        Regex("^(?:सब\\s*हटाओ|सब\\s*हटा\\s*दो|सब\\s*मिटाओ|बिल\\s*खाली\\s*करो)$", RegexOption.IGNORE_CASE)
    )

    private val REMOVE_PREFIX_PATTERNS = listOf(
        Regex("^(?:please\\s+)?(?:remove|delete|drop|cancel)\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(?:తొలగించు|తీసివేయి|డిలీట్)\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(?:हटाओ|मिटाओ|डिलीट\\s*करो)\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    private val REMOVE_SUFFIX_PATTERNS = listOf(
        Regex("^(.+?)\\s+(?:remove|delete|drop)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)\\s*(?:తొలగించు|తీసివేయి)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)\\s*(?:हटाओ|मिटाओ)$", RegexOption.IGNORE_CASE)
    )

    private val REVIEW_PATTERNS = listOf(
        Regex("^(?:review\\s+bill|show\\s+bill|view\\s+bill|checkout|finish\\s+bill)$", RegexOption.IGNORE_CASE),
        Regex("^(?:బిల్లు\\s*సమీక్ష|బిల్లు\\s*చూపించు|బిల్లు\\s*చూడు)$", RegexOption.IGNORE_CASE),
        Regex("^(?:बिल\\s*दिखाओ|बिल\\s*देखो|फाइनल\\s*करो)$", RegexOption.IGNORE_CASE)
    )

    private val PRINT_PATTERNS = listOf(
        Regex("^(?:print\\s+bill|print\\s+invoice|print)$", RegexOption.IGNORE_CASE),
        Regex("^(?:ప్రింట్\\s*చేయి|రశీదు\\s*ప్రింట్)$", RegexOption.IGNORE_CASE),
        Regex("^(?:प्रिंट\\s*करो|रसीद\\s*प्रिंट)$", RegexOption.IGNORE_CASE)
    )

    private val NEW_BILL_PATTERNS = listOf(
        Regex("^(?:new\\s+bill|fresh\\s+bill|start\\s+new\\s+bill)$", RegexOption.IGNORE_CASE),
        Regex("^(?:కొత్త\\s*బిల్లు|మరొక\\s*బిల్లు)$", RegexOption.IGNORE_CASE),
        Regex("^(?:नया\\s*बिल|अगला\\s*बिल)$", RegexOption.IGNORE_CASE)
    )

    fun parseUtterance(text: String, existingItems: List<BillItem> = emptyList()): VoiceActionResult {
        val clean = text.trim()
        if (clean.isBlank()) return VoiceActionResult.None

        // 1. Check Operational Commands
        for (pat in CLEAR_PATTERNS) {
            if (pat.containsMatchIn(clean)) return VoiceActionResult.ClearAll(clean)
        }
        for (pat in REMOVE_PREFIX_PATTERNS) {
            pat.find(clean)?.let { m ->
                val target = m.groupValues[1].trim()
                if (target.isNotBlank()) return VoiceActionResult.RemoveItem(target)
            }
        }
        for (pat in REMOVE_SUFFIX_PATTERNS) {
            pat.find(clean)?.let { m ->
                val target = m.groupValues[1].trim()
                if (target.isNotBlank()) return VoiceActionResult.RemoveItem(target)
            }
        }
        for (pat in REVIEW_PATTERNS) {
            if (pat.containsMatchIn(clean)) return VoiceActionResult.ReviewBill
        }
        for (pat in PRINT_PATTERNS) {
            if (pat.containsMatchIn(clean)) return VoiceActionResult.PrintBill
        }
        for (pat in NEW_BILL_PATTERNS) {
            if (pat.containsMatchIn(clean)) return VoiceActionResult.NewBill
        }

        // 2. Check Contextual Price or Quantity Update on Existing Items
        if (existingItems.isNotEmpty()) {
            // Price only update e.g. "60 rupees", "₹60", "price is 60"
            val priceOnlyRegex = Regex("^(?:price\\s*is\\s*|rate\\s*is\\s*|ధర\\s*|రేటు\\s*|कीमत\\s*|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|rupees?|రూపాయలు|रुपये)?$", RegexOption.IGNORE_CASE)
            val priceMatch = priceOnlyRegex.find(clean)
            if (priceMatch != null) {
                val p = priceMatch.groupValues[1].toDoubleOrNull()
                if (p != null && p > 0) {
                    val missingItem = existingItems.lastOrNull { it.isMissingPrice || it.price == null }
                    return VoiceActionResult.PriceUpdate(missingItem?.name, p)
                }
            }

            // Item targeted price update e.g. "rice 60 rupees", "rice price is 60", "soap 35"
            val itemPriceRegex = Regex("^(.+?)\\s+(?:price\\s*is|rate\\s*is|ధర|कीमत)?\\s*(?:at|is|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|rupees?|రూపాయలు|रुपये)?$", RegexOption.IGNORE_CASE)
            val itemPriceMatch = itemPriceRegex.find(clean)
            if (itemPriceMatch != null) {
                val targetName = itemPriceMatch.groupValues[1].trim()
                val priceVal = itemPriceMatch.groupValues[2].toDoubleOrNull()
                if (priceVal != null && priceVal > 0) {
                    val matched = existingItems.find { it.name.contains(targetName, ignoreCase = true) || targetName.contains(it.name, ignoreCase = true) }
                    if (matched != null) {
                        return VoiceActionResult.PriceUpdate(matched.name, priceVal)
                    }
                }
            }

            // Quantity only update e.g. "500 g", "2 kg", "3 packets"
            val qtyOnlyRegex = Regex("^(\\d+(?:\\.\\d+)?)\\s*(kg|kilos?|g|grams?|l|litres?|ml|packets?|pkts?|pcs|pieces?|bottles?)$", RegexOption.IGNORE_CASE)
            val qtyMatch = qtyOnlyRegex.find(clean)
            if (qtyMatch != null) {
                val q = qtyMatch.groupValues[1].toDoubleOrNull() ?: 1.0
                val u = ArithmeticEngine.normalizeUnit(qtyMatch.groupValues[2])
                return VoiceActionResult.QuantityUpdate(q, u)
            }
        }

        // 3. Multi-Item Extraction
        val extracted = extractItemsFromSpeech(clean)
        return if (extracted.isNotEmpty()) {
            VoiceActionResult.AddItems(extracted)
        } else {
            VoiceActionResult.None
        }
    }

    private fun extractItemsFromSpeech(utterance: String): List<BillItem> {
        val normalized = normalizeRegionalFractionsAndNumbers(utterance)
        // Split on common connectors: "and", ",", ";", "మరియు", "ఔర్", "aur"
        val clauses = normalized.split(Regex("(?:\\band\\b|,|;|\\bమరియు\\b|\\bऔर\\b|\\baur\\b)", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val items = mutableListOf<BillItem>()
        for (clause in clauses) {
            val item = parseSingleClause(clause)
            if (item != null && isValidItemName(item.name)) {
                items.add(item)
            }
        }
        return items
    }

    private fun normalizeRegionalFractionsAndNumbers(input: String): String {
        var s = input
        // Telugu Fractions
        s = s.replace(Regex("అరకేజీ|అర\\s*కిలో|అర\\s*కేజీ", RegexOption.IGNORE_CASE), " 0.5 kg ")
        s = s.replace(Regex("పావు\\s*కేజీ|పావు\\s*కిలో", RegexOption.IGNORE_CASE), " 0.25 kg ")
        s = s.replace(Regex("ముప్పావు\\s*కేజీ|ముప్పావు\\s*కిలో", RegexOption.IGNORE_CASE), " 0.75 kg ")
        s = s.replace(Regex("ఒకటిన్నర\\s*కిలో", RegexOption.IGNORE_CASE), " 1.5 kg ")
        s = s.replace(Regex("రెండున్నర\\s*కిలో", RegexOption.IGNORE_CASE), " 2.5 kg ")

        // Hindi Fractions
        s = s.replace(Regex("आधा\\s*किलो|आधा\\s*kg", RegexOption.IGNORE_CASE), " 0.5 kg ")
        s = s.replace(Regex("पाव\\s*किलो|एक\\s*पाव", RegexOption.IGNORE_CASE), " 0.25 kg ")
        s = s.replace(Regex("डेढ़\\s*किलो", RegexOption.IGNORE_CASE), " 1.5 kg ")
        s = s.replace(Regex("ढाई\\s*किलो", RegexOption.IGNORE_CASE), " 2.5 kg ")

        // English Fractions & Number words
        s = s.replace(Regex("half\\s+(?:a\\s+)?kilo(?:gram)?", RegexOption.IGNORE_CASE), " 0.5 kg ")
        s = s.replace(Regex("quarter\\s+kilo(?:gram)?", RegexOption.IGNORE_CASE), " 0.25 kg ")
        s = s.replace(Regex("one\\s+and\\s+a\\s+half\\s+kilo(?:gram)?", RegexOption.IGNORE_CASE), " 1.5 kg ")

        // Compound Thousands
        s = s.replace(Regex("(\\d+)\\s*(?:thousand|thaosand|hazar|వేలు|हजार)\\s*(\\d+)", RegexOption.IGNORE_CASE)) { m ->
            val thousands = m.groupValues[1].toIntOrNull() ?: 1
            val hundreds = m.groupValues[2].toIntOrNull() ?: 0
            (thousands * 1000 + hundreds).toString()
        }
        s = s.replace(Regex("(\\d+)\\s*(?:thousand|thaosand|hazar|వేలు|हजार)", RegexOption.IGNORE_CASE)) { m ->
            val thousands = m.groupValues[1].toIntOrNull() ?: 1
            (thousands * 1000).toString()
        }
        return s
    }

    private fun parseSingleClause(clause: String): BillItem? {
        val c = clause.replace(Regex("[.,;!?]"), "").trim()
        if (c.isBlank()) return null

        // Pattern: <Quantity> <Unit> <Name> <Price>
        // Example: "2 kg rice 60 rupees" / "500 g sugar 45"
        val pat1 = Regex("^(\\d+(?:\\.\\d+)?)\\s*(kg|g|gm|l|ltr|ml|pkt|packet|bag|box|pcs|pieces?|bottle)\\s+(?:of\\s+)?(.+?)(?:\\s+(?:at|rate|price|for|రూపాయలు|रुपये|rs\\.?|rupees?)?\\s*(\\d+(?:\\.\\d+)?)(?:\\s*(?:rs\\.?|rupees?|/-))?)?$", RegexOption.IGNORE_CASE)
        val m1 = pat1.find(c)
        if (m1 != null) {
            val qty = m1.groupValues[1].toDoubleOrNull() ?: 1.0
            val unit = ArithmeticEngine.normalizeUnit(m1.groupValues[2])
            val name = cleanItemName(m1.groupValues[3])
            val priceStr = m1.groupValues[4]
            val price = if (priceStr.isNotBlank()) priceStr.toDoubleOrNull() else null

            return BillItem(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = qty,
                unit = unit,
                price = price,
                priceUnit = if (unit == "g") "kg" else if (unit == "ml") "L" else unit,
                priceBasis = if (unit in listOf("bottle", "pkt", "pcs", "box")) PriceBasis.PACK_PRICE else PriceBasis.UNIT_RATE,
                isMissingPrice = (price == null),
                inputSource = "voice"
            )
        }

        // Pattern: <Name> <Quantity> <Unit> <Price>
        // Example: "Rice 2 kg 120" / "Sugar 500 g 50" / "Toor dal 1 kg"
        val pat2 = Regex("^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(kg|g|gm|l|ltr|ml|pkt|packet|bag|box|pcs|pieces?|bottle)(?:\\s+(?:at|rate|price|for|రూపాయలు|रुपये|rs\\.?|rupees?)?\\s*(\\d+(?:\\.\\d+)?)(?:\\s*(?:rs\\.?|rupees?|/-))?)?$", RegexOption.IGNORE_CASE)
        val m2 = pat2.find(c)
        if (m2 != null) {
            val name = cleanItemName(m2.groupValues[1])
            val qty = m2.groupValues[2].toDoubleOrNull() ?: 1.0
            val unit = ArithmeticEngine.normalizeUnit(m2.groupValues[3])
            val priceStr = m2.groupValues[4]
            val price = if (priceStr.isNotBlank()) priceStr.toDoubleOrNull() else null

            return BillItem(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = qty,
                unit = unit,
                price = price,
                priceUnit = if (unit == "g") "kg" else if (unit == "ml") "L" else unit,
                priceBasis = if (unit in listOf("bottle", "pkt", "pcs", "box")) PriceBasis.PACK_PRICE else PriceBasis.UNIT_RATE,
                isMissingPrice = (price == null),
                inputSource = "voice"
            )
        }

        // Pattern: <Name> <Price> (e.g. "Cooking oil 150", "Lifebuoy soap 35")
        val pat3 = Regex("^(.+?)\\s+(?:at|rate|price|for|రూపాయలు|रुपये|rs\\.?|rupees?|₹)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|rupees?|/-)?$", RegexOption.IGNORE_CASE)
        val m3 = pat3.find(c)
        if (m3 != null) {
            val name = cleanItemName(m3.groupValues[1])
            val price = m3.groupValues[2].toDoubleOrNull()
            return BillItem(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = 1.0,
                unit = "pcs",
                price = price,
                priceUnit = "pcs",
                priceBasis = PriceBasis.PACK_PRICE,
                isMissingPrice = (price == null),
                inputSource = "voice"
            )
        }

        // Fallback: Standalone product name with missing price (e.g. "Sunflower Oil", "Rice", "Sugar")
        val nameOnly = cleanItemName(c)
        if (isValidItemName(nameOnly)) {
            return BillItem(
                id = UUID.randomUUID().toString(),
                name = nameOnly,
                quantity = 1.0,
                unit = "pcs",
                price = null,
                priceUnit = "pcs",
                priceBasis = PriceBasis.PACK_PRICE,
                isMissingPrice = true,
                inputSource = "voice"
            )
        }
        return null
    }

    private fun cleanItemName(raw: String): String {
        var s = raw.trim().replace(Regex("^(?:a|an|the|some|of)\\s+", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:rupees?|rs\\.?|రూపాయలు|रुपये|at|for|each)\\b", RegexOption.IGNORE_CASE), "").trim()
        return s.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun isValidItemName(name: String): Boolean {
        if (name.isBlank() || name.length < 2) return false
        val invalid = setOf("kg", "g", "l", "ml", "pcs", "pkt", "rs", "rupees", "rupee", "rate", "price", "bill", "cost", "రూపాయలు", "रुपये")
        return !invalid.contains(name.lowercase()) && !name.matches(Regex("^\\d+(?:\\.\\d+)?$"))
    }
}

