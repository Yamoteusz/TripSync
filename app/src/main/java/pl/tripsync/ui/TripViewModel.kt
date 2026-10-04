package pl.tripsync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.tripsync.AppGraph
import pl.tripsync.data.*
import pl.tripsync.domain.*

data class AppState(val session: Session? = null, val records: List<RecordEntity> = emptyList(), val pending: List<OperationEntity> = emptyList(), val conflicts: List<ConflictEntity> = emptyList(), val sync: String = "Dane dostępne offline", val loaded: Boolean = false)

class TripViewModel(val graph: AppGraph) : ViewModel() {
    val repo = graph.repository
    private val travel = TravelRepository(graph.context)
    val locations = MutableStateFlow<List<GeoResult>>(emptyList())
    val geoMessage = MutableStateFlow<String?>(null)
    val weather = MutableStateFlow(WeatherState())
    private var weatherJob: kotlinx.coroutines.Job? = null
    fun searchLocations(query: String) = action {
        locations.value = emptyList()
        geoMessage.value = null
        try {
            locations.value = travel.search(query)
            if (locations.value.isEmpty()) geoMessage.value = "Nie znaleziono miejscowości. Spróbuj krótszej nazwy."
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            geoMessage.value = "Wyszukiwanie niedostępne. Sprawdź internet lub dodaj współrzędne ręcznie."
        }
    }
    fun loadWeather(lat: Double, lon: Double) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            weather.value = WeatherState(loading = true)
            val cached = travel.cached(lat,lon)
            weather.value = WeatherState(cached, loading = true)
            try { weather.value = WeatherState(travel.forecast(lat,lon)) }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                weather.value = WeatherState(cached, message = if(cached == null) "Nie udało się pobrać prognozy. Sprawdź połączenie i spróbuj ponownie." else "Brak połączenia z API. Wyświetlamy ostatnią zapisaną prognozę; sprawdź jej daty.")
            }
        }
    }
    val state = combine(repo.session, repo.records, repo.operations, repo.conflicts, repo.sync) { s,r,p,c,status -> AppState(s,r,p,c,status,true) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())
    val dark = graph.prefs.dark.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val url = graph.prefs.url.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    fun action(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                message.value = repo.errorMessage(e)
            } finally { busy.value = false }
        }
    }
    fun sync() = action { repo.sync() }
    fun logout() = action { repo.logout() }
    fun <T> decode(record: RecordEntity, clazz: Class<T>) = repo.decode(record, clazz)
    fun records(trip: String, kind: String) = state.value.records.filter { it.tripId == trip && it.kind == kind }
    fun owner(trip: String): Boolean = records(trip, "member").any { decode(it, MemberData::class.java).let { m -> m.userId == state.value.session?.user?.id && m.role == "owner" } }
    class Factory(private val graph: AppGraph) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(graph) as T
        }
    }
}
