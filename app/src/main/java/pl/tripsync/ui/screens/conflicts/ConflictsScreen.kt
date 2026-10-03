package pl.tripsync.ui.screens.conflicts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.tripsync.data.*
import pl.tripsync.domain.*


import pl.tripsync.ui.*

@Composable
fun ConflictsScreen(vm: TripViewModel,state: AppState) {
    Page("Zmiany wymagające uwagi","Porównaj wersje i wybierz, co ma pozostać.") {
        if(state.conflicts.isEmpty()) EmptyState(Icons.Default.CheckCircle,"Wszystko rozwiązane","Nie ma konfliktów ani odrzuconych zmian.")
        state.conflicts.forEach {c ->
            val local=state.records.find {it.id==c.recordId}
            val remote=if(c.serverJson.isNotBlank()) vm.repo.gson.fromJson(c.serverJson,RemoteRecord::class.java) else null
            Panel {
                Icon(Icons.Default.SyncProblem,null,tint=MaterialTheme.colorScheme.error)
                Text(c.message,fontWeight=FontWeight.Bold)
                Text("Twoja wersja",style=MaterialTheme.typography.labelLarge)
                val pending=state.pending.find {it.recordId==c.recordId}
                Text(if(pending?.deleted==true) "Usunięcie elementu" else local?.let {friendly(vm,it.kind,it.json)} ?: "Zmiana oczekująca",style=MaterialTheme.typography.bodySmall)
                Text("Wersja serwera",style=MaterialTheme.typography.labelLarge)
                Text(if(remote==null) "Brak rekordu na serwerze" else if(remote.deleted) "Element został usunięty" else friendly(vm,remote.kind,vm.repo.gson.toJson(remote.payload)),style=MaterialTheme.typography.bodySmall)
                if(!c.invalid && remote?.deleted!=true) Button(onClick={vm.action {vm.repo.resolve(c.recordId,true)}}) {Text("Zachowaj moją wersję")}
                OutlinedButton(onClick={vm.action {vm.repo.resolve(c.recordId,false)}}) {Text(if(remote==null) "Odrzuć moją zmianę" else "Przyjmij wersję serwera")}
            }
        }
    }
}

private fun friendly(vm: TripViewModel,kind: String,json: String): String = when(kind) {
    "trip" -> vm.repo.gson.fromJson(json,TripData::class.java).let {"${it.title}\n${it.destination}\n${it.startDate} - ${it.endDate}\n${it.currency}\n${it.description}"}
    "event" -> vm.repo.gson.fromJson(json,EventData::class.java).let {"${it.title}\n${it.date} ${it.time}\n${it.place}\n${it.description}\n${if(it.done) "Wykonane" else "Do realizacji"}"}
    "task" -> vm.repo.gson.fromJson(json,TaskData::class.java).let {"${it.title}\n${if(it.category=="packing") "Pakowanie" else "Zadanie"}\n${if(it.done) "Wykonane" else "Do wykonania"}"}
    "proposal" -> vm.repo.gson.fromJson(json,ProposalData::class.java).let {"${it.title}\n${it.place}\n${it.description}"}
    "expense" -> vm.repo.gson.fromJson(json,ExpenseData::class.java).let {"${it.title}\n${java.math.BigDecimal.valueOf(it.amount,2)}\n${it.date}"}
    "member" -> vm.repo.gson.fromJson(json,MemberData::class.java).name
    else -> "Głos na propozycję"
}
