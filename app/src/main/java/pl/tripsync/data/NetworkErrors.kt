package pl.tripsync.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun apiErrorMessage(error: Exception): String {
    if (error is HttpException) {
        val fallback = "Błąd serwera (${error.code()}). Spróbuj ponownie."
        return runCatching {
            Gson().fromJson(error.response()?.errorBody()?.string(), JsonObject::class.java)
                ?.get("message")?.asString?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: fallback
    }
    val causes = generateSequence(error as Throwable) { it.cause }.take(8).toList()
    return when {
        causes.any { it is UnknownHostException } -> "Nie można odnaleźć domeny serwera (DNS). Sprawdź adres API i internet urządzenia."
        causes.any { it is SSLException } -> "Nie udało się zweryfikować bezpiecznego połączenia HTTPS. Sprawdź adres API oraz datę i godzinę urządzenia."
        causes.any { it is SocketTimeoutException } -> "Serwer nie odpowiedział na czas. Sprawdź połączenie i spróbuj ponownie."
        causes.any { it is ConnectException } -> "Nie można połączyć się z serwerem. Sprawdź adres API i port."
        error is IOException -> "Brak połączenia z serwerem. Dane są dostępne offline."
        else -> error.message ?: "Nie udało się wykonać operacji."
    }
}
