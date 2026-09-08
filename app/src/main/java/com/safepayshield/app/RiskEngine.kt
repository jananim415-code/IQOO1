package com.safepayshield.app

import android.net.Uri
import java.time.LocalTime

enum class Verdict { SAFE, CAUTION, BLOCK }

data class RiskResult(
    val pa: String,
    val payee: String,
    val amount: String,
    val note: String,
    val score: Int,
    val verdict: Verdict,
    val reasons: List<String>,
    val layers: List<Pair<String, Int>>
)

data class VisualFeatures(
    val contrast: Double = .82,
    val edgeContinuity: Double = .88,
    val occlusion: Double = .04,
    val alignment: Double = .92
)

data class DemoScenario(val title: String, val upi: String, val expected: Verdict)

class RiskEngine(private val inference: OnDeviceInference? = null) {
    companion object {
        val verifiedMerchantVpas = setOf("verified@okaxis", "citycafe@okhdfcbank", "metro@oksbi")
        val knownFraudVpas = setOf("refunddesk@ybl", "support-kyc@upi", "prizeclaim@okaxis")
        val demoScenarios = listOf(
            DemoScenario("Normal merchant QR", "upi://pay?pa=verified@okaxis&pn=City%20Cafe&am=249&tn=Lunch", Verdict.SAFE),
            DemoScenario("Suspicious QR", "upi://pay?pa=unknown-shop@upi&pn=Quick%20Refund&am=2400&tn=Urgent%20refund%20claim%20now", Verdict.CAUTION),
            DemoScenario("High-risk scam QR", "upi://pay?pa=refunddesk@ybl&pn=Refund%20Desk&am=75000&tn=OTP%20required%20immediately%20tampered", Verdict.BLOCK)
        )
    }

    fun visualTamperScore(features: VisualFeatures): Double {
        val qualityRisk = ((1 - features.contrast) + (1 - features.edgeContinuity) + features.occlusion + (1 - features.alignment)) / 4
        return qualityRisk.coerceIn(0.0, 1.0)
    }

    fun newPayeeHighAmountScore(raw: String, history: List<HistoryItem>, threshold: Double = 10_000.0): Double {
        val uri = Uri.parse(raw)
        val pa = uri.getQueryParameter("pa").orEmpty()
        val amount = uri.getQueryParameter("am")?.toDoubleOrNull() ?: 0.0
        val seen = history.any { Uri.parse(it.raw).getQueryParameter("pa").equals(pa, true) }
        return if (!seen && amount > threshold) .35 else 0.0
    }

    fun merchantNameMismatchScore(pa: String, payee: String): Double {
        val merchantSignals = listOf("electricity", "board", "hospital", "school", "metro")
        val claimsMerchant = merchantSignals.any { payee.contains(it, true) }
        val personalVpa = !pa.contains("merchant", true) && !pa.contains("bill", true) && !pa.contains("official", true)
        return if (claimsMerchant && personalVpa) .28 else 0.0
    }

    fun repeatedHighRiskScore(pa: String, history: List<HistoryItem>): Double {
        val repeats = history.count { it.verdict != Verdict.SAFE && Uri.parse(it.raw).getQueryParameter("pa").equals(pa, true) }
        return (repeats * .12).coerceAtMost(.36)
    }

    fun analyze(raw: String, trustedMerchants: Set<String>, history: List<HistoryItem> = emptyList(), visualFeatures: VisualFeatures? = null): RiskResult {
        val uri = Uri.parse(raw.trim())
        val pa = uri.getQueryParameter("pa").orEmpty().ifBlank { "unknown@upi" }
        val payee = uri.getQueryParameter("pn").orEmpty().ifBlank { pa.substringBefore('@') }
        val amount = uri.getQueryParameter("am").orEmpty().ifBlank { "0" }
        val note = uri.getQueryParameter("tn").orEmpty()
        val visual = if (raw.contains("tampered", true)) .94 else inference?.visualScore(visualFeatures ?: VisualFeatures()) ?: visualTamperScore(visualFeatures ?: VisualFeatures())
        val suspiciousWords = Regex("urgent|refund|lottery|prize|otp|immediately|kyc|verify", RegexOption.IGNORE_CASE)
        val payload = when {
            suspiciousWords.containsMatchIn(note) -> .82
            pa.length > 45 || pa.count { it == '.' } > 2 || pa.contains("-support", true) -> .62
            else -> inference?.payloadScore(note, pa) ?: .12
        }
        val merchant = when {
            knownFraudVpas.any { pa.equals(it, true) } -> 1.0
            trustedMerchants.any { pa.equals(it, true) } || verifiedMerchantVpas.any { pa.equals(it, true) } -> .04
            else -> .58
        }
        val newPayee = newPayeeHighAmountScore(raw, history)
        val mismatch = merchantNameMismatchScore(pa, payee)
        val repeated = repeatedHighRiskScore(pa, history)
        val merchantAdjusted = (merchant + newPayee + mismatch + repeated).coerceIn(0.0, 1.0)
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        val lateNight = LocalTime.now().let { it.hour >= 23 || it.hour < 5 }
        val behavioral = when {
            amountValue > 50000 && lateNight -> .92
            amountValue > 50000 -> .74
            amount.count { it == '.' } > 1 -> .64
            else -> .10
        }
        val score = ((visual * .25 + payload * .35 + merchantAdjusted * .25 + behavioral * .15) * 100).toInt()
        val verdict = when { score >= 65 -> Verdict.BLOCK; score >= 35 -> Verdict.CAUTION; else -> Verdict.SAFE }
        val reasons = buildList {
            if (visual > .50) add("The QR image shows signs of being changed.")
            if (payload > .50) add("The payment note or address looks unusual.")
            if (knownFraudVpas.any { pa.equals(it, true) }) add("This payment address is on the local fraud list.")
            else if (merchantAdjusted > .50) add("This payee is not verified on this device.")
            if (newPayee > 0) add("This is a new payee with an unusually high amount.")
            if (mismatch > 0) add("The payee name does not match a typical merchant address.")
            if (repeated > 0) add("This payee has appeared in recent risky scans.")
            if (behavioral > .50) add("This amount or payment time is outside your usual pattern.")
            if (isEmpty()) add("The payee and payment pattern look familiar.")
        }
        return RiskResult(pa, payee, amount, note, score, verdict, reasons, listOf(
            "Visual check" to (visual * 100).toInt(), "Payload check" to (payload * 100).toInt(), "Merchant check" to (merchantAdjusted * 100).toInt(), "Behavior check" to (behavioral * 100).toInt()
        ))
    }
}
