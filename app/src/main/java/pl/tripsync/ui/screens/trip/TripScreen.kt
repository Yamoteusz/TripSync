@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.trip

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

import pl.tripsync.ui.screens.plan.PlanContent

import pl.tripsync.ui.screens.tasks.TasksContent

import pl.tripsync.ui.screens.ideas.IdeasContent

import pl.tripsync.ui.screens.money.MoneyContent

@Composable
fun TripScreen(vm: TripViewModel,state: AppState,nav: NavHostController,tripId: String,tab: String) {
    val record=state.records.find {it.id==tripId && it.kind=="trip"}
    if(record==null) { Page("Wyjazd niedostępny") { Text("Wyjazd usunięto lub cofnięto dostęp."); Button(onClick={nav.navigate("trips") {popUpTo("trips"){inclusive=true}}}) {Text("Wróć do wyjazdów")} }; return }
    val t=vm.decode(record,TripData::class.java)
    val rows=state.records.filter {it.tripId==tripId}
    val members=rows.filter {it.kind=="member"}.map {vm.decode(it,MemberData::class.java)}
    val owner=members.any {it.userId==state.session?.user?.id && it.role=="owner"}
    val name:(String)->String={id -> members.find {it.userId==id}?.name ?: "Były uczestnik"}
    val events=rows.filter {it.kind=="event"}.sortedWith(compareBy({vm.decode(it,EventData::class.java).date},{vm.decode(it,EventData::class.java).rank},{vm.decode(it,EventData::class.java).time}))
    val tasks=rows.filter {it.kind=="task"}
    val expenses=rows.filter {it.kind=="expense"}
    val context=LocalContext.current
    var deleting by rememberSaveable {mutableStateOf(false)}
    val editor:(String,String)->Unit={kind,id -> nav.navigate("edit/$tripId/$kind/$id")}
    Page(when(tab) {"plan"->"Plan podróży"; "tasks"->"Zadania i pakowanie";"ideas"->"Pomysły grupy";"money"->"Wspólne koszty";else->t.title},"${t.destination} · ${Dates.range(t.startDate,t.endDate)}",positionKey="$tripId/$tab",positions=vm.scrollPositions) {
        when(tab) {
            "overview" -> {
                SyncCard(state,nav)
                TravelHero("${members.size} osób · ${events.size} punktów planu",t.destination,t.description.ifBlank {"Wasz czas. Wasza przygoda. Wszystko, czego potrzebujecie, jest tutaj."})
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Stat("Wydatki",Money.format(expenses.sumOf {vm.decode(it,ExpenseData::class.java).amount},t.currency),Modifier.weight(1f))
                    Stat("Zadania", "${tasks.count {vm.decode(it,TaskData::class.java).done}} / ${tasks.size}",Modifier.weight(1f))
                }
                Panel {
                    Text("Najbliżej w planie",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    val next=events.filter {val e=vm.decode(it,EventData::class.java);!e.done && runCatching {java.time.LocalDateTime.parse("${e.date}T${e.time}")>=java.time.LocalDateTime.now()}.getOrDefault(false)}.minByOrNull {val e=vm.decode(it,EventData::class.java);"${e.date}T${e.time}"}
                    if(next!=null) {val e=vm.decode(next,EventData::class.java);Text(e.title);Text("${Dates.display(e.date)} · ${e.time} · ${e.place}",style=MaterialTheme.typography.bodySmall)} else Text("Brak nadchodzących punktów. Sprawdź harmonogram lub zaplanuj coś nowego.")
                    TextButton(onClick={nav.navigate("trip/$tripId/plan") {popUpTo("trips")}}) {Text("Zobacz harmonogram")}
                }
                ActionTile("Mapa miejsc i pogoda","Zapisane miejsca i prognoza na 7 dni",Icons.Default.Map) {nav.navigate("places/$tripId")}
                ActionTile("Historia zmian","Zobacz, co zmieniło się w grupie",Icons.Default.History) {nav.navigate("activity/$tripId")}
                ActionTile("Uczestnicy i zaproszenia","Zaproś ekipę do wspólnego planowania",Icons.Default.Group) {nav.navigate("members/$tripId")}
                OutlinedButton(onClick={share(context,buildString {append("${t.title}\n${t.destination}\n${Dates.range(t.startDate,t.endDate)}\n"); events.forEach {val e=vm.decode(it,EventData::class.java);append("\n${Dates.display(e.date)} ${e.time} · ${e.title} · ${e.place}")}})},Modifier.fillMaxWidth()) {Text("Udostępnij plan")}
                if(owner) {OutlinedButton(onClick={editor("trip",tripId)},Modifier.fillMaxWidth()) {Text("Edytuj wyjazd")}; TextButton(onClick={deleting=true},Modifier.fillMaxWidth()) {Text("Usuń wyjazd",color=MaterialTheme.colorScheme.error)}}
            }
            "plan" -> PlanContent(vm,tripId,events,editor)
            "tasks" -> TasksContent(vm,tripId,tasks,name,editor)
            "ideas" -> IdeasContent(vm,state,nav,tripId,rows,editor)
            "money" -> MoneyContent(vm,state,nav,tripId,expenses,t,name,editor)
        }
    }
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Usunąć cały wyjazd?")},text={Text("Plan, zadania i wydatki zostaną usunięte dla wszystkich uczestników po synchronizacji.")},confirmButton={TextButton(onClick={vm.action {vm.repo.deleteTrip(tripId);deleting=false;nav.navigate("trips") {popUpTo("trips"){inclusive=true}}}}) {Text("Usuń",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleting=false}) {Text("Anuluj")}})
}

