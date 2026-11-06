package pl.tripsync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import pl.tripsync.data.apiErrorMessage
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class NetworkErrorsTest {
    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )

    @Test fun dnsFailureIsDistinguishedWithoutExposingExceptionDetails() {
        val message = apiErrorMessage(UnknownHostException("private-network-details"))
        assertTrue(message.contains("DNS"))
        assertFalse(message.contains("private-network-details"))
    }
    @Test fun wrappedDnsFailureIsRecognized() {
        assertTrue(apiErrorMessage(IOException("request failed", UnknownHostException())).contains("DNS"))
    }
    @Test fun certificateFailureIsDistinguishedFromOffline() {
        assertTrue(apiErrorMessage(SSLHandshakeException("certificate rejected")).contains("HTTPS"))
    }
    @Test fun timeoutAndConnectionRefusalHaveSpecificMessages() {
        assertTrue(apiErrorMessage(SocketTimeoutException()).contains("na czas"))
        assertTrue(apiErrorMessage(ConnectException()).contains("port"))
    }
    @Test fun serverValidationMessageIsPreserved() {
        assertEquals("Nieprawidłowy e-mail lub hasło.", apiErrorMessage(httpError(401, """{"message":"Nieprawidłowy e-mail lub hasło."}""")))
    }
    @Test fun malformedAndEmptyServerErrorsFallBackToStatusCode() {
        for (body in listOf("<html>Bad Gateway</html>", "{}", "null", """{"message":""}""")) {
            assertTrue(apiErrorMessage(httpError(502, body)).contains("502"))
        }
    }
    @Test fun validationAndUnknownIoErrorsRemainReadable() {
        assertEquals("Podaj poprawny e-mail.", apiErrorMessage(IllegalArgumentException("Podaj poprawny e-mail.")))
        assertTrue(apiErrorMessage(IOException()).contains("offline"))
    }
}
