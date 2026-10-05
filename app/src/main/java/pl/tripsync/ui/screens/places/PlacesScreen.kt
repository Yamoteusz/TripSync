package pl.tripsync.ui.screens.places

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.gson.Gson
import pl.tripsync.domain.*
import pl.tripsync.ui.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlacesScreen(vm: TripViewModel, state: AppState, nav: NavHostController, tripId: String) {
    val trip = state.records.find { it.id == tripId } ?: return
    val t = vm.decode(trip,TripData::class.java)
    val rows = state.records.filter { it.tripId == tripId && it.kind == "place" }
    val places = rows.map { vm.decode(it,PlaceData::class.java) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    LaunchedEffect(tripId) {vm.locations.value=emptyList();vm.geoMessage.value=null}
    val results by vm.locations.collectAsStateWithLifecycle()
    val geoMessage by vm.geoMessage.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    var query by rememberSaveable(tripId) { mutableStateOf(t.destination.substringBefore(',')) }
    var forecastName by rememberSaveable(tripId) { mutableStateOf("") }
    var latitude by rememberSaveable(tripId) { mutableDoubleStateOf(0.0) }
    var longitude by rememberSaveable(tripId) { mutableDoubleStateOf(0.0) }
    val context = LocalContext.current
    val selected = if(forecastName.isNotBlank()) PlaceData(forecastName,latitude,longitude) else null
    LaunchedEffect(tripId,forecastName) {
        if(forecastName.isNotBlank() && weather.forecast == null && !weather.loading) vm.loadWeather(latitude,longitude)
    }
    Page("Mapa miejsc i pogoda", "Wspólne zapisane miejsca oraz wyszukiwanie miejscowości. Mapy i świeża pogoda wymagają internetu.") {
        AddButton("Dodaj miejsce ręcznie") { nav.navigate("place/$tripId/new") }
        Panel {
            Field("Szukaj miejscowości", query, { query = it })
            Button(onClick = { vm.searchLocations(query) }, modifier = Modifier.fillMaxWidth(),enabled=!busy && query.trim().length>=2) { Text("Wyszukaj miejscowość") }
            geoMessage?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            results.forEach { result ->
                Text(result.label,style=MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = { vm.action { vm.repo.save(tripId,"place",PlaceData(result.label.take(120),result.latitude,result.longitude)); vm.message.value="Miejsce zapisane." } }) { Text("Zapisz miejsce") }
                    TextButton(onClick = { forecastName=result.label;latitude=result.latitude;longitude=result.longitude;vm.loadWeather(latitude,longitude) }) { Text("Pogoda") }
                }
            }
            Text("Wyszukiwarka zwraca miejscowości. Dla zabytków lub szlaków dodaj dokładne współrzędne ręcznie.",style=MaterialTheme.typography.bodySmall)
        }
        LeafletMap(places + listOfNotNull(selected), Modifier.fillMaxWidth().height(330.dp))
        Text("© OpenStreetMap contributors · Leaflet. Mapa bez pobierania obszarów offline.",style=MaterialTheme.typography.bodySmall)
        if(places.isEmpty()) Text("Zapisz pierwsze miejsce, aby zobaczyć je na mapie.")
        rows.forEach { row ->
            val p=vm.decode(row,PlaceData::class.java)
            Panel {
                Text(p.title,style=MaterialTheme.typography.titleMedium)
                Text("${p.latitude}, ${p.longitude}",style=MaterialTheme.typography.bodySmall)
                if(p.description.isNotBlank()) Text(p.description)
                Row {
                    TextButton(onClick={forecastName=p.title;latitude=p.latitude;longitude=p.longitude;vm.loadWeather(latitude,longitude)}) {Text("Pogoda")}
                    TextButton(onClick={nav.navigate("place/$tripId/${row.id}")}) {Text("Edytuj miejsce")}
                }
                OutlinedButton(onClick={vm.action {
                    val uri="geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}(${Uri.encode(p.title)})".toUri()
                    val intent=Intent(Intent.ACTION_VIEW,uri)
                    try {context.startActivity(intent)} catch (_: android.content.ActivityNotFoundException) {context.startActivity(Intent(Intent.ACTION_VIEW,"https://www.openstreetmap.org/?mlat=${p.latitude}&mlon=${p.longitude}#map=15/${p.latitude}/${p.longitude}".toUri()))}
                }},modifier=Modifier.fillMaxWidth()) {Text("Otwórz w aplikacji map")}
            }
        }
        if(forecastName.isNotBlank()) Panel {
            Text("Prognoza: $forecastName",style=MaterialTheme.typography.titleMedium)
            Text("Najbliższe 7 dni, w strefie miejsca. Prognoza nie obejmuje odległych terminów wyjazdu.",style=MaterialTheme.typography.bodySmall)
            if(weather.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            weather.message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            weather.forecast?.takeIf { it.latitude==latitude && it.longitude==longitude }?.let { cached ->
                Text("Pobrano: ${Instant.ofEpochMilli(cached.fetchedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",style=MaterialTheme.typography.bodySmall)
                val daily=cached.response.daily
                daily.time.forEachIndexed { i,date ->
                    Divider()
                    Text("${Dates.day(date)} · ${pl.tripsync.data.weatherLabel(daily.codes.getOrElse(i){-1})}")
                    Text("${daily.minimum.getOrNull(i) ?: "–"}–${daily.maximum.getOrNull(i) ?: "–"} °C · szansa opadów ${daily.rain.getOrNull(i)?.let { "$it%" } ?: "brak danych"}",style=MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick={vm.loadWeather(latitude,longitude)}) {Text("Odśwież prognozę")}
            Text("Dane: Open-Meteo (CC BY 4.0). Prognoza modelowa, nie pomiar bieżący.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun LeafletMap(places: List<PlaceData>, modifier: Modifier) {
    val context=LocalContext.current
    var mapState by remember {mutableStateOf("loading")}
    var generation by remember {mutableIntStateOf(0)}
    LaunchedEffect(places,generation) {mapState="loading";delay(15000);if(mapState=="loading") mapState="error"}
    val css=remember {context.assets.open("leaflet/leaflet.css").bufferedReader().use {it.readText()}}
    val js=remember {context.assets.open("leaflet/leaflet.js").bufferedReader().use {it.readText()}}
    val html=remember(places) {
        val json=Gson().toJson(places)
        """<!doctype html><html><head><title>map-loading</title><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src https://tile.openstreetmap.org https://*.tile.openstreetmap.org data:;"><style>$css html,body{margin:0;padding:0}#map{height:330px;width:100%}</style></head><body><div id="map" role="img" aria-label="Mapa zapisanych miejsc"></div><script>$js
const points=$json;const map=L.map('map').setView([52.1,19.4],5);let failed=0;L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).on('loading',()=>{failed=0;document.title='map-loading'}).on('tileerror',()=>{failed++;document.title='map-error'}).on('load',()=>{document.title=failed?'map-error':'map-ready'}).addTo(map);points.forEach(p=>{const label=document.createElement('span');label.textContent=p.title;L.circleMarker([p.latitude,p.longitude],{radius:9,color:'#126a68',fillOpacity:0.8}).addTo(map).bindPopup(label)});if(points.length===1)map.setView([points[0].latitude,points[0].longitude],13);else if(points.length>1)map.fitBounds(points.map(p=>[p.latitude,p.longitude]),{padding:[24,24],maxZoom:15});</script></body></html>"""
    }
    Box(modifier.clip(RoundedCornerShape(24.dp))) {
    key(generation) {AndroidView(factory={ c -> WebView(c).apply {
        if(pl.tripsync.BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        settings.javaScriptEnabled=true
        settings.allowFileAccess=false
        settings.allowContentAccess=false
        settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString="TripSync/${pl.tripsync.BuildConfig.VERSION_NAME} Android " + settings.userAgentString
        webChromeClient=object:WebChromeClient() {
            override fun onReceivedTitle(view:WebView,title:String?) {when(title) {"map-loading" -> mapState="loading";"map-ready" -> mapState="ready";"map-error" -> mapState="error"}}
        }
        webViewClient=object:WebViewClient() {
            override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError) {if(request.isForMainFrame) mapState="error"}
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                if(request.url.toString()=="https://tripsync.local/") return false
                if(request.url.toString()=="https://www.openstreetmap.org/copyright") runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,request.url))}
                return true
            }
        }
    } },update={ view -> if(view.tag!=html) {view.tag=html;view.loadDataWithBaseURL("https://tripsync.local/",html,"text/html","UTF-8",null)} },modifier=Modifier.fillMaxSize(),onRelease={it.stopLoading();it.destroy()})}
    if(mapState=="loading") Surface(Modifier.align(Alignment.TopCenter).padding(12.dp),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Text("Ładowanie mapy",style=MaterialTheme.typography.labelMedium)}
    }
    if(mapState=="error") Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {Text("Nie udało się pobrać mapy. Zapisane miejsca są nadal dostępne.",style=MaterialTheme.typography.bodySmall);TextButton(onClick={mapState="loading";generation++}) {Text("Ponów ładowanie mapy")}}
    }
    }
}
