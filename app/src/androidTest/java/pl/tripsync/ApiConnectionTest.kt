package pl.tripsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ApiConnectionTest {
    @Test fun healthAndAuthenticationRoutesAreReachable() {
        val base = InstrumentationRegistry.getArguments().getString("apiUrl") ?: "http://10.0.2.2:8080/"
        val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
        client.newCall(Request.Builder().url(base.trimEnd('/') + "/health").build()).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("ok", JsonParser.parseString(response.body!!.string()).asJsonObject.get("status").asString)
        }
        client.newCall(Request.Builder().url(base.trimEnd('/') + "/sync").build()).execute().use { response ->
            assertEquals(401, response.code)
        }
    }
    @Test fun invalidCredentialsReachLoginEndpoint() {
        val base = InstrumentationRegistry.getArguments().getString("apiUrl") ?: "http://10.0.2.2:8080/"
        val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
        val json = """{"email":"diagnostic-${UUID.randomUUID()}@example.invalid","password":"diagnostic-invalid-password"}"""
        val request = Request.Builder().url(base.trimEnd('/') + "/auth/login")
            .post(json.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            assertEquals(401, response.code)
            assertEquals("Nieprawidłowy e-mail lub hasło.", JsonParser.parseString(response.body!!.string()).asJsonObject.get("message").asString)
        }
    }
}
