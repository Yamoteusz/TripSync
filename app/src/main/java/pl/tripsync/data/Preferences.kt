package pl.tripsync.data

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pl.tripsync.BuildConfig
import pl.tripsync.domain.Session
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settings by preferencesDataStore("settings")

class Preferences(private val context: Context) {
    private val urlKey = stringPreferencesKey("api_url")
    private val darkKey = booleanPreferencesKey("dark")
    val url = context.settings.data.map { it[urlKey] ?: if (BuildConfig.DEBUG) "http://10.0.2.2:8080/" else "" }
    val dark = context.settings.data.map { it[darkKey] ?: false }
    suspend fun url() = url.first()
    suspend fun setUrl(value: String) {
        val uri = try { URI(value.trim()) } catch (_: Exception) { throw IllegalArgumentException("Nieprawidłowy adres API.") }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) { "Podaj pełny adres API." }
        require(uri.scheme == "https" || (BuildConfig.DEBUG && uri.scheme == "http" && uri.host in listOf("10.0.2.2", "localhost", "127.0.0.1"))) { "Wymagany jest HTTPS. W wersji debug HTTP jest dostępne tylko lokalnie." }
        context.settings.edit { it[urlKey] = value.trim().trimEnd('/') + "/" }
    }
    suspend fun setDark(value: Boolean) { context.settings.edit { it[darkKey] = value } }
}

@SuppressLint("ApplySharedPref", "UseKtx")
class SessionVault(context: Context, name: String = "vault") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val gson = Gson()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("tripsync-session", null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder("tripsync-session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
    fun write(session: Session) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).putString("data", Base64.encodeToString(data, Base64.NO_WRAP)).commit())
    }
    fun read(): Session? = try {
        val data = prefs.getString("data", null)
        val iv = prefs.getString("iv", null)
        if (data == null || iv == null) null else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            gson.fromJson(String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8), Session::class.java)
        }
    } catch (_: Exception) { clear(); null }
    fun clear() { prefs.edit().clear().commit() }
}
