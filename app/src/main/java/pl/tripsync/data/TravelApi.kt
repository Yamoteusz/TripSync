package pl.tripsync.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeoResult(val id: Long = 0, val name: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0, val country: String? = null, val admin1: String? = null) {
    val label get() = listOfNotNull(name, admin1, country).joinToString(", ")
}
data class GeoResponse(val results: List<GeoResult>? = null)
data class ForecastDaily(val time: List<String> = emptyList(), @SerializedName("temperature_2m_max") val maximum: List<Double> = emptyList(), @SerializedName("temperature_2m_min") val minimum: List<Double> = emptyList(), @SerializedName("precipitation_probability_max") val rain: List<Int?> = emptyList(), @SerializedName("weather_code") val codes: List<Int> = emptyList())
data class ForecastResponse(val daily: ForecastDaily = ForecastDaily(), val timezone: String = "")
data class CachedForecast(val latitude: Double, val longitude: Double, val fetchedAt: Long, val response: ForecastResponse)
data class WeatherState(val forecast: CachedForecast? = null, val loading: Boolean = false, val message: String? = null)

interface GeocodingApi {
    @GET("v1/search") suspend fun search(@Query("name") query: String, @Query("count") count: Int = 8, @Query("language") language: String = "pl"): GeoResponse
}
interface ForecastApi {
    @GET("v1/forecast") suspend fun forecast(@Query("latitude") latitude: Double, @Query("longitude") longitude: Double, @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max", @Query("timezone") timezone: String = "auto", @Query("forecast_days") days: Int = 7): ForecastResponse
}

class TravelRepository(context: Context, forecastOverride: ForecastApi? = null) {
    private val gson = Gson()
    private val cache = context.getSharedPreferences("weather-cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private fun retrofit(url: String) = Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create(gson)).build()
    private val geo = retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)
    private val weather = forecastOverride ?: retrofit("https://api.open-meteo.com/").create(ForecastApi::class.java)
    suspend fun search(query: String): List<GeoResult> {
        require(query.trim().length in 2..120) { "Podaj nazwę miejscowości (2–120 znaków)." }
        return geo.search(query.trim()).results ?: emptyList()
    }
    private fun key(lat: Double, lon: Double) = String.format(java.util.Locale.ROOT, "%.4f:%.4f", lat, lon)
    suspend fun cached(lat: Double, lon: Double): CachedForecast? = withContext(Dispatchers.IO) { runCatching { gson.fromJson(cache.getString(key(lat,lon), null), CachedForecast::class.java) }.getOrNull() }
    suspend fun forecast(lat: Double, lon: Double): CachedForecast {
        require(lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0) { "Nieprawidłowe współrzędne." }
        val value = CachedForecast(lat, lon, System.currentTimeMillis(), weather.forecast(lat,lon))
        require(value.response.daily.time.isNotEmpty()) { "Brak prognozy dla tego miejsca." }
        withContext(Dispatchers.IO) { cache.edit(commit=true) {putString(key(lat,lon),gson.toJson(value))} }
        return value
    }
}

fun weatherLabel(code: Int) = when (code) {
    0 -> "Bezchmurnie"
    1, 2 -> "Częściowe zachmurzenie"
    3 -> "Pochmurno"
    45, 48 -> "Mgła"
    51, 53, 55, 56, 57 -> "Mżawka"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Deszcz"
    71, 73, 75, 77, 85, 86 -> "Śnieg"
    95, 96, 99 -> "Burza"
    else -> "Warunki zmienne"
}
