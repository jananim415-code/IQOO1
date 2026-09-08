package com.safepayshield.app

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("shield_local", Context.MODE_PRIVATE)
    private val alias = "safepay_local_aes"

    init {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(alias)) {
            KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(android.security.keystore.KeyGenParameterSpec.Builder(alias,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build())
                generateKey()
            }
        }
    }

    private fun key(): SecretKey = (KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        .getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey

    fun get(key: String, fallback: String = ""): String {
        val stored = prefs.getString(key, null) ?: return fallback
        return runCatching {
            val bytes = Base64.decode(stored, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }.getOrDefault(fallback)
    }

    fun put(key: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val result = cipher.iv + cipher.doFinal(value.toByteArray())
        prefs.edit().putString(key, Base64.encodeToString(result, Base64.NO_WRAP)).apply()
    }
}
