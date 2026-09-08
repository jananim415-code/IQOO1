package com.safepayshield.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryItem(val raw: String, val payee: String, val verdict: Verdict, val score: Int)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SecureStore(app)
    private val engine = RiskEngine(OnDeviceInference(app))
    private val _result = MutableStateFlow<RiskResult?>(null)
    val result = _result.asStateFlow()
    private val _language = MutableStateFlow(store.get("language", "English"))
    val language = _language.asStateFlow()
    private val _elderMode = MutableStateFlow(store.get("elder", "false") == "true")
    val elderMode = _elderMode.asStateFlow()
    private val _familyApproval = MutableStateFlow(store.get("family", "false") == "true")
    val familyApproval = _familyApproval.asStateFlow()
    private val _contacts = MutableStateFlow(store.get("contacts", "Maya, +91 98765 43210").split(", ").filter(String::isNotBlank))
    val contacts = _contacts.asStateFlow()
    private val _merchants = MutableStateFlow(store.get("merchants", "verified@okaxis").split(", ").filter(String::isNotBlank).toSet())
    val merchants = _merchants.asStateFlow()
    private val _history = MutableStateFlow(readHistory())
    val history = _history.asStateFlow()
    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction = _lastAction.asStateFlow()
    private val _familyApproved = MutableStateFlow(store.get("family_approved", "false") == "true")
    val familyApproved = _familyApproved.asStateFlow()
    private val _clearEvent = MutableStateFlow(0)
    val clearEvent = _clearEvent.asStateFlow()

    fun analyze(raw: String) {
        val clean = raw.trim()
        if (!clean.startsWith("upi://pay", true)) return
        val analyzed = engine.analyze(clean, _merchants.value, _history.value)
        _result.value = analyzed
        _familyApproved.value = false
        store.put("family_approved", "false")
        _history.value = (listOf(HistoryItem(clean, analyzed.payee, analyzed.verdict, analyzed.score)) + _history.value).distinctBy { it.raw }.take(5)
        saveHistory()
    }
    fun runScenario(index: Int) { RiskEngine.demoScenarios.getOrNull(index)?.let { analyze(it.upi) } }
    fun recordAction(action: String) { _lastAction.value = action; store.put("last_action", action) }
    fun approveFamily(result: RiskResult): Boolean {
        if (_contacts.value.isEmpty() || result.verdict == Verdict.BLOCK) return false
        _familyApproved.value = true
        store.put("family_approved", "true")
        recordAction("Family approval recorded")
        return true
    }
    fun canProceed(result: RiskResult): Boolean = result.verdict != Verdict.BLOCK &&
        (!familyApproval.value || result.verdict != Verdict.CAUTION || familyApproved.value)
    fun hasTrustedContact(): Boolean = _contacts.value.isNotEmpty()
    fun removeContact(value: String) { _contacts.value = _contacts.value - value; store.put("contacts", _contacts.value.joinToString(", ")) }
    fun removeMerchant(value: String) { _merchants.value = _merchants.value - value; store.put("merchants", _merchants.value.joinToString(",")) }
    fun clearLocalData() {
        _history.value = emptyList(); _lastAction.value = null; _contacts.value = emptyList(); _merchants.value = emptySet(); _familyApproved.value = false
        store.put("contacts", ""); store.put("merchants", ""); store.put("history", ""); store.put("family_approved", "false")
        _clearEvent.value += 1
    }
    private fun readHistory(): List<HistoryItem> = store.get("history").split("\n").mapNotNull { line ->
        val parts = line.split("|", limit = 4)
        if (parts.size == 4) runCatching { HistoryItem(Uri.decode(parts[0]), parts[1], Verdict.valueOf(parts[2]), parts[3].toInt()) }.getOrNull() else null
    }
    private fun saveHistory() { store.put("history", _history.value.joinToString("\n") { "${Uri.encode(it.raw)}|${it.payee}|${it.verdict.name}|${it.score}" }) }
    fun clearResult() { _result.value = null }
    fun setLanguage(value: String) { _language.value = value; store.put("language", value) }
    fun setElder(value: Boolean) { _elderMode.value = value; store.put("elder", value.toString()) }
    fun setFamily(value: Boolean) { _familyApproval.value = value; store.put("family", value.toString()) }
    fun addContact(value: String) { if (value.isNotBlank()) { _contacts.value += value; store.put("contacts", _contacts.value.joinToString(", ")) } }
    fun addMerchant(value: String) { if (value.isNotBlank()) { _merchants.value += value; store.put("merchants", _merchants.value.joinToString(",")) } }
}
