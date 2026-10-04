package pl.tripsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pl.tripsync.data.*
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WeatherCacheTest {
    @Test fun cachedForecastSurvivesRepositoryRestartAndNetworkFailure(): Unit = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        var calls=0
        val successful=object:ForecastApi {
            override suspend fun forecast(latitude:Double,longitude:Double,daily:String,timezone:String,days:Int):ForecastResponse {
                calls++
                return ForecastResponse(ForecastDaily(listOf("2026-10-04"),listOf(12.0),listOf(3.0),listOf(50),listOf(61)),"Europe/Warsaw")
            }
        }
        val lat=12.3456;val lon=23.4567
        val saved=TravelRepository(context,successful).forecast(lat,lon)
        assertEquals(1,calls)
        val offline=object:ForecastApi {
            override suspend fun forecast(latitude:Double,longitude:Double,daily:String,timezone:String,days:Int):ForecastResponse {throw IOException("Offline test")}
        }
        val recreated=TravelRepository(context,offline)
        assertEquals(saved,recreated.cached(lat,lon))
        try {recreated.forecast(lat,lon);fail("Expected network failure")} catch (_:IOException) {}
        assertEquals(saved,recreated.cached(lat,lon))
        assertNull(recreated.cached(13.3456,lon))
        context.getSharedPreferences("weather-cache",android.content.Context.MODE_PRIVATE).edit().remove("12.3456:23.4567").commit()
    }
}
