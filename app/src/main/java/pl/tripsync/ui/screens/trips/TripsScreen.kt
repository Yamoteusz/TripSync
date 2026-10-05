@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.trips

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import pl.tripsync.data.RecordEntity
import pl.tripsync.domain.*


import pl.tripsync.ui.*

@Composable
fun TripsScreen(vm: TripViewModel,state: AppState,nav: NavHostController,onJoin: ()->Unit) {
    val trips=state.records.filter {it.kind=="trip"}.sortedBy {vm.decode(it,TripData::class.java).startDate}
    var query by rememberSaveable {mutableStateOf("")}
    var archive by rememberSaveable {mutableStateOf(false)}
    Page("Twoje wyjazdy","Cześć, ${state.session?.user?.name}. Dokąd tym razem?") {
        TravelHero("WSPÓLNY KIERUNEK", "Dobre chwile.\nDobry plan.", "Zbierz ekipę. Zaplanuj podróż.\nWspólne koszty ogarnij po drodze.")
        SyncCard(state,nav)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick={nav.navigate("edit/new/trip/new")},Modifier.weight(1f)) { Icon(Icons.Default.Add,null); Text("Nowy wyjazd") }
            OutlinedButton(onClick=onJoin,Modifier.weight(1f),enabled=state.session?.demo==false) { Text("Dołącz kodem") }
        }
        if(trips.isEmpty()) EmptyState(Icons.Default.Luggage,"Pierwsza przygoda czeka","Utwórz wyjazd i zacznij planować. Wszystkie zmiany zapiszemy na urządzeniu.")
        if(trips.isNotEmpty()) {
            SectionTitle("Wasze podróże", "Liczba wyjazdów: ${trips.size}")
            Field("Szukaj wyjazdu",query,{query=it})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!archive,onClick={archive=false},label={Text("Aktywne")})
                FilterChip(selected=archive,onClick={archive=true},label={Text("Zakończone")})
            }
        }
        val visible=trips.filter { val item=vm.decode(it,TripData::class.java); val ended=runCatching {java.time.LocalDate.parse(item.endDate)<java.time.LocalDate.now()}.getOrDefault(false); ended==archive && (item.title.contains(query,true) || item.destination.contains(query,true)) }
        if(trips.isNotEmpty() && visible.isEmpty()) EmptyState(Icons.Default.Search,"Brak wyjazdów w tej kategorii","Zmień kategorię lub wyszukiwaną nazwę.")
        visible.forEach { r ->
            val t=vm.decode(r,TripData::class.java)
            Card(onClick={nav.navigate("trip/${r.id}/overview")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Landscape,null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(t.title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); Icon(Icons.Default.ChevronRight,"Otwórz wyjazd") }
                    Text(t.destination,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {Icon(Icons.Default.CalendarMonth,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Text(Dates.range(t.startDate,t.endDate),style=MaterialTheme.typography.bodySmall)}
                    Text("${state.records.count {it.tripId==r.id && it.kind=="member"}} uczestników · ${t.currency}${if(r.pending) " · oczekuje na zapis w chmurze" else ""}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

