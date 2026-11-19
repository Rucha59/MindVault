package com.mindvault.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Stores the Supabase session encrypted (AES-256-GCM, key in the Android Keystore) instead of the
 * library's default plain SharedPreferences. If the data cannot be decrypted (for example after a
 * restore to another device) it is discarded and the user simply signs in again.
 * The preferences file is excluded from backup (see res/xml/backup_rules.xml).
 */
@Singleton
class EncryptedSessionManager @Inject constructor(
    @ApplicationContext context: Context,
) : SessionManager {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        val plain = json.encodeToString(UserSession.serializer(), session)
        prefs.edit().putString(KEY_SESSION, encrypt(plain)).commit()
        Unit
    }

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val stored = prefs.getString(KEY_SESSION, null) ?: return@withContext null
        try {
            json.decodeFromString(UserSession.serializer(), decrypt(stored))
        } catch (e: GeneralSecurityException) {
            discard()
        } catch (e: IllegalArgumentException) {
            discard()
        } catch (e: kotlinx.serialization.SerializationException) {
            discard()
        }
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_SESSION).commit()
        Unit
    }

    private fun discard(): UserSession? {
        prefs.edit().remove(KEY_SESSION).commit()
        return null
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        require(bytes.size > IV_SIZE_BYTES) { "Stored session is too short" }
        val iv = bytes.copyOfRange(0, IV_SIZE_BYTES)
        val body = bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "mindvault_auth"
        const val KEY_SESSION = "session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mindvault_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
    }
}