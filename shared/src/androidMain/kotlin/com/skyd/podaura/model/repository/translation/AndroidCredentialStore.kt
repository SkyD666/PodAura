package com.skyd.podaura.model.repository.translation

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidCredentialStore(context: Context) : CredentialStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun put(id: String, secret: String) {
        if (secret.isBlank()) throw CredentialStorageException()
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(secret.encodeToByteArray())
            val value = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
            check(preferences.edit().putString(storageKey(id), value).commit())
        }.getOrElse { throw CredentialStorageException() }
    }

    override suspend fun get(id: String): String? {
        val stored = preferences.getString(storageKey(id), null) ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            require(bytes.size > IV_SIZE_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_SIZE_BITS, bytes.copyOfRange(0, IV_SIZE_BYTES)),
            )
            cipher.doFinal(bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)).decodeToString()
        }.getOrElse { throw CredentialStorageException() }
    }

    override suspend fun delete(id: String) {
        if (!preferences.edit().remove(storageKey(id)).commit()) throw CredentialStorageException()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun storageKey(id: String): String = MessageDigest.getInstance("SHA-256")
        .digest(id.encodeToByteArray())
        .joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    private companion object {
        const val PREFERENCES_NAME = "translation_credentials_encrypted"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "podaura.translation.credentials.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_SIZE_BITS = 128
    }
}
