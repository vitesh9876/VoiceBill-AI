package com.example.voicebillai.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicebillai.data.VoiceBillRepository
import com.example.voicebillai.engine.ArithmeticEngine
import com.example.voicebillai.engine.SoundboxTtsService
import com.example.voicebillai.engine.VoiceActionResult
import com.example.voicebillai.engine.VoiceParserEngine
import com.example.voicebillai.model.Bill
import com.example.voicebillai.model.BillItem
import com.example.voicebillai.model.DiscountType
import com.example.voicebillai.model.PaymentMode
import com.example.voicebillai.model.ShopSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppTab {
    BILLING,
    HISTORY,
    KHATA,
    SETTINGS
}

data class SpeechState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val error: String? = null
)

class VoiceBillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceBillRepository(application)
    private val ttsService = SoundboxTtsService(application)
    private var speechRecognizer: SpeechRecognizer? = null

    val activeBill: StateFlow<Bill> = repository.activeBill
    val history: StateFlow<List<Bill>> = repository.history
    val settings: StateFlow<ShopSettings> = repository.settings

    private val _speechState = MutableStateFlow(SpeechState())
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.BILLING)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _showCheckout = MutableStateFlow(false)
    val showCheckout: StateFlow<Boolean> = _showCheckout.asStateFlow()

    private val _editingItem = MutableStateFlow<BillItem?>(null)
    val editingItem: StateFlow<BillItem?> = _editingItem.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        val ctx = getApplication<Application>()
        if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _speechState.value = _speechState.value.copy(isListening = true, error = null)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _speechState.value = _speechState.value.copy(isListening = false)
                }

                override fun onError(error: Int) {
                    _speechState.value = _speechState.value.copy(isListening = false, error = "Speech error ($error)")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    _speechState.value = _speechState.value.copy(isListening = false, transcript = text)
                    if (text.isNotBlank()) {
                        processVoiceUtterance(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) {
                        _speechState.value = _speechState.value.copy(transcript = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.value.speechLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        _speechState.value = SpeechState(isListening = true, transcript = "")
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState(isListening = false, error = e.localizedMessage)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _speechState.value = _speechState.value.copy(isListening = false)
    }

    fun processVoiceUtterance(utterance: String) {
        val currentItems = activeBill.value.items
        val action = VoiceParserEngine.parseUtterance(utterance, currentItems)

        when (action) {
            is VoiceActionResult.ClearAll -> {
                clearBill()
                speakFeedback("Cleared all items from bill")
                postMessage("Cleared all items")
            }
            is VoiceActionResult.RemoveItem -> {
                val target = action.itemName.lowercase()
                val updated = activeBill.value.items.filterNot { it.name.lowercase().contains(target) }
                repository.updateActiveBill(activeBill.value.copy(items = updated))
                speakFeedback("Removed ${action.itemName}")
                postMessage("Removed ${action.itemName}")
            }
            is VoiceActionResult.ReviewBill -> {
                if (ArithmeticEngine.isReadyToFinalize(activeBill.value)) {
                    _showCheckout.value = true
                    speakFeedback("Reviewing bill. Grand total ₹${activeBill.value.grandTotal}")
                } else {
                    postMessage("Please enter missing prices first")
                    speakFeedback("Please enter missing prices before finalizing")
                }
            }
            is VoiceActionResult.PrintBill -> {
                _showCheckout.value = true
            }
            is VoiceActionResult.NewBill -> {
                startNewBill()
                speakFeedback("Started new bill")
            }
            is VoiceActionResult.PriceUpdate -> {
                val target = action.targetName?.lowercase()
                val items = activeBill.value.items.toMutableList()
                val idx = if (target != null) {
                    items.indexOfLast { it.name.lowercase().contains(target) }
                } else {
                    items.indexOfLast { it.isMissingPrice || it.price == null }
                }
                if (idx != -1) {
                    val itm = items[idx]
                    items[idx] = itm.copy(price = action.price, isMissingPrice = false)
                    repository.recordItemPrice(itm.name, action.price)
                    repository.updateActiveBill(activeBill.value.copy(items = items))
                    speakFeedback("Set price for ${itm.name} to ₹${action.price}")
                    postMessage("Updated ${itm.name} price: ₹${action.price}")
                }
            }
            is VoiceActionResult.QuantityUpdate -> {
                val items = activeBill.value.items.toMutableList()
                if (items.isNotEmpty()) {
                    val lastIdx = items.lastIndex
                    val last = items[lastIdx]
                    items[lastIdx] = last.copy(quantity = action.quantity, unit = action.unit)
                    repository.updateActiveBill(activeBill.value.copy(items = items))
                    speakFeedback("Updated ${last.name} to ${action.quantity} ${action.unit}")
                }
            }
            is VoiceActionResult.AddItems -> {
                val enhanced = action.items.map { itm ->
                    // Check if Last-Known Price memory can suggest a price
                    val suggested = repository.getSuggestedPrice(itm.name)
                    if (itm.price == null && suggested != null) {
                        itm.copy(suggestedPrice = suggested)
                    } else {
                        itm
                    }
                }
                val updatedItems = activeBill.value.items + enhanced
                repository.updateActiveBill(activeBill.value.copy(items = updatedItems))

                // Kirana Soundbox Audio Feedback
                val first = enhanced.firstOrNull()
                if (first != null) {
                    if (first.price != null) {
                        speakFeedback("${first.name} added. Line total ₹${ArithmeticEngine.calculateLineAmount(first) ?: first.price}")
                    } else if (first.suggestedPrice != null && settings.value.enableLastKnownPrice) {
                        speakFeedback("${first.name} added. Last price was ₹${first.suggestedPrice}")
                    } else {
                        speakFeedback("${first.name} added. Please tell price")
                    }
                }
            }
            VoiceActionResult.None -> {
                postMessage("Could not understand: \"$utterance\"")
            }
        }
    }

    private fun speakFeedback(text: String) {
        if (settings.value.enableSoundboxFeedback) {
            ttsService.setLanguage(settings.value.speechLang)
            ttsService.speak(text)
        }
    }

    fun applySuggestedPrice(item: BillItem) {
        val suggested = item.suggestedPrice ?: return
        val updated = activeBill.value.items.map {
            if (it.id == item.id) it.copy(price = suggested, isMissingPrice = false) else it
        }
        repository.recordItemPrice(item.name, suggested)
        repository.updateActiveBill(activeBill.value.copy(items = updated))
        speakFeedback("Applied ₹$suggested for ${item.name}")
    }

    fun setItemPrice(itemId: String, price: Double) {
        val updated = activeBill.value.items.map {
            if (it.id == itemId) {
                repository.recordItemPrice(it.name, price)
                it.copy(price = price, isMissingPrice = false)
            } else it
        }
        repository.updateActiveBill(activeBill.value.copy(items = updated))
    }

    fun removeItem(itemId: String) {
        val updated = activeBill.value.items.filterNot { it.id == itemId }
        repository.updateActiveBill(activeBill.value.copy(items = updated))
    }

    fun saveItem(item: BillItem) {
        val existing = activeBill.value.items
        val idx = existing.indexOfFirst { it.id == item.id }
        val updated = if (idx >= 0) {
            existing.toMutableList().apply { set(idx, item) }
        } else {
            existing + item
        }
        if (item.price != null) {
            repository.recordItemPrice(item.name, item.price)
        }
        repository.updateActiveBill(activeBill.value.copy(items = updated))
        _editingItem.value = null
    }

    fun clearBill() {
        repository.updateActiveBill(activeBill.value.copy(items = emptyList()))
    }

    fun startNewBill() {
        repository.startNewBill()
        _showCheckout.value = false
    }

    fun setDiscount(type: DiscountType, value: Double) {
        repository.updateActiveBill(activeBill.value.copy(discountType = type, discountValue = value))
    }

    fun setTaxRate(rate: Double) {
        repository.updateActiveBill(activeBill.value.copy(taxRate = rate))
    }

    fun setCustomerInfo(name: String, phone: String) {
        repository.updateActiveBill(activeBill.value.copy(customerName = name, customerPhone = phone))
    }

    fun finalizeBill(paymentMode: PaymentMode): Boolean {
        val bill = activeBill.value
        val total = bill.grandTotal
        val success = repository.finalizeActiveBill(paymentMode)
        if (success) {
            _showCheckout.value = false
            speakFeedback("Bill finalized. Total ₹$total collected via ${paymentMode.name}")
            postMessage("Bill saved successfully!")
        }
        return success
    }

    fun updateSettings(newSettings: ShopSettings) {
        repository.updateSettings(newSettings)
    }

    fun setTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun showCheckoutDialog(show: Boolean) {
        _showCheckout.value = show
    }

    fun editItem(item: BillItem?) {
        _editingItem.value = item
    }

    fun postMessage(msg: String) {
        _userFeedbackMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_userFeedbackMessage.value == msg) {
                _userFeedbackMessage.value = null
            }
        }
    }

    fun getKhataLedger() = repository.getKhataLedger()

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        ttsService.shutdown()
    }
}

