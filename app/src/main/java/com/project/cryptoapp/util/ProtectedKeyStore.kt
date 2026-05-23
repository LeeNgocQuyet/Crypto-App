package com.project.cryptoapp.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

data class ProtectedKeyPair(
    val privateKey: String,
    val publicKey: String,
)

class ProtectedKeyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveKeyPair(privateKey: String, publicKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedPrivateKey = cipher.doFinal(privateKey.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_PRIVATE_CIPHERTEXT, encryptedPrivateKey.toBase64())
            .putString(KEY_PRIVATE_IV, cipher.iv.toBase64())
            .putString(KEY_PUBLIC, publicKey)
            .apply()
    }

    fun loadKeyPair(): ProtectedKeyPair? {
        val encrypted = preferences.getString(KEY_PRIVATE_CIPHERTEXT, null)?.fromBase64() ?: return null
        val iv = preferences.getString(KEY_PRIVATE_IV, null)?.fromBase64() ?: return null
        val publicKey = preferences.getString(KEY_PUBLIC, null) ?: return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        val privateKey = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        return ProtectedKeyPair(privateKey = privateKey, publicKey = publicKey)
    }

    fun hasKeyPair(): Boolean =
        preferences.contains(KEY_PRIVATE_CIPHERTEXT) &&
            preferences.contains(KEY_PRIVATE_IV) &&
            preferences.contains(KEY_PUBLIC)

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "crypto_app_private_key_wrap_v1"
        const val PREFS_NAME = "protected_ecc_keys"
        const val KEY_PRIVATE_CIPHERTEXT = "private_key_ciphertext"
        const val KEY_PRIVATE_IV = "private_key_iv"
        const val KEY_PUBLIC = "public_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val AES_KEY_BITS = 256
    }
}
