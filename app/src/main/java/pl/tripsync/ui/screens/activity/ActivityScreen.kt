package pl.tripsync.ui.screens.activity

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import pl.tripsync.domain.ActivityData
import pl.tripsync.ui.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityScreen(vm: TripViewModel, state: AppState, tripId: String) {
    val items = state.records.filter { it.tripId == tripId && it.kind == "activity" }.map { vm.decode(it, ActivityData::class.java) }.sortedByDescending { it.at }
    Page("Historia zmian", "Autor i czas zmian zaakceptowanych przez serwer. Zmiany offline pojawią się po synchronizacji.") {
        if (items.isEmpty()) Text("Brak zapisanych zmian.")
        items.take(200).forEach { a ->
            Panel {
                Text("${a.actorName} · ${when (a.action) { "convert" -> "przeniesiono do planu"; "create" -> "dodano"; "update" -> "zmieniono"; "delete" -> "usunięto"; "join" -> "dołączono"; "invite" -> "zaproszenie"; else -> a.action }}", style = MaterialTheme.typography.titleMedium)
                val kind = mapOf("trip" to "Wyjazd", "budget" to "Budżet", "expense" to "Wydatek", "settlement" to "Spłata", "place" to "Miejsce", "event" to "Plan", "task" to "Zadanie", "proposal" to "Pomysł", "vote" to "Głos", "member" to "Uczestnik")[a.recordKind] ?: a.recordKind
                Text("$kind · ${a.title}")
                Text(Instant.ofEpochMilli(a.at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (items.size > 200) Text("Wyświetlono 200 najnowszych wpisów; pełne dane pozostają na serwerze.")
    }
}
