@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import pl.tripsync.data.*
import pl.tripsync.domain.*
import java.time.LocalDate
import java.time.LocalDateTime


import pl.tripsync.ui.*

@Composable
fun EditorScreen(vm: TripViewModel,state: AppState,nav: NavHostController,tripId: String,kind: String,recordId: String,requestPermission: ()->Unit) {
    val existing=state.records.find {it.id==recordId}
    val tripRecord=state.records.find {it.id==tripId && it.kind=="trip"}
    val trip=tripRecord?.let {vm.decode(it,TripData::class.java)} ?: TripData()
    val members=state.records.filter {it.tripId==tripId && it.kind=="member"}.map {vm.decode(it,MemberData::class.java)}
    val oldTrip=if(kind=="trip" && existing!=null) vm.decode(existing,TripData::class.java) else TripData()
    val oldEvent=if(kind=="event" && existing!=null) vm.decode(existing,EventData::class.java) else EventData(date=trip.startDate,rank=state.records.count {it.tripId==tripId && it.kind=="event"})
    val oldTask=if(kind=="task" && existing!=null) vm.decode(existing,TaskData::class.java) else TaskData(date=trip.startDate)
    val oldProposal=if(kind in listOf("proposal","convert") && existing!=null) vm.decode(existing,ProposalData::class.java) else ProposalData()
    val oldExpense=if(kind=="expense" && existing!=null) vm.decode(existing,ExpenseData::class.java) else ExpenseData(date=trip.startDate,payer=state.session?.user?.id ?: "")
    var title by rememberSaveable(recordId,kind) {mutableStateOf(when(kind) {"trip"->oldTrip.title;"event"->oldEvent.title;"task"->oldTask.title;"expense"->oldExpense.title;else->oldProposal.title})}
    var destination by rememberSaveable(recordId,kind) {mutableStateOf(oldTrip.destination)}
    var date by rememberSaveable(recordId,kind) {mutableStateOf(when(kind) {"trip"->oldTrip.startDate;"event"->oldEvent.date;"task"->oldTask.date;"expense"->oldExpense.date;else->trip.startDate})}
    var endDate by rememberSaveable(recordId,kind) {mutableStateOf(oldTrip.endDate)}
    var time by rememberSaveable(recordId,kind) {mutableStateOf(if(kind=="task") oldTask.time else oldEvent.time)}
    var place by rememberSaveable(recordId,kind) {mutableStateOf(if(kind=="event") oldEvent.place else oldProposal.place)}
    var description by rememberSaveable(recordId,kind) {mutableStateOf(when(kind) {"trip"->oldTrip.description;"event"->oldEvent.description;else->oldProposal.description})}
    var currency by rememberSaveable(recordId,kind) {mutableStateOf(oldTrip.currency)}
    var category by rememberSaveable(recordId,kind) {mutableStateOf(oldTask.category)}
    var assignee by rememberSaveable(recordId,kind) {mutableStateOf(oldTask.assignee)}
    var reminder by rememberSaveable(recordId,kind) {mutableStateOf(if(kind=="task") oldTask.reminder else oldEvent.reminder)}
    var amount by rememberSaveable(recordId,kind) {mutableStateOf(if(oldExpense.amount>0) java.math.BigDecimal.valueOf(oldExpense.amount,2).toPlainString() else "")}
    var payer by rememberSaveable(recordId,kind) {mutableStateOf(oldExpense.payer)}
    var selected by rememberSaveable(recordId,kind) {mutableStateOf(if(oldExpense.shares.isNotEmpty()) oldExpense.shares.keys.toList() else members.map {it.userId})}
    var custom by rememberSaveable(recordId,kind) {mutableStateOf(oldExpense.amount > 0 && oldExpense.shares.isNotEmpty() && oldExpense.shares != Money.equalSplit(oldExpense.amount,oldExpense.shares.keys.toList()))}
    var sharesText by rememberSaveable(recordId,kind) {mutableStateOf(members.map {java.math.BigDecimal.valueOf(oldExpense.shares[it.userId] ?: 0,2).toPlainString()})}
    var delete by rememberSaveable {mutableStateOf(false)}
    val busy by vm.busy.collectAsStateWithLifecycle()
    if(kind!="trip" && tripRecord==null) {Page("Wyjazd niedostępny") {Text("Wróć do listy wyjazdów.")};return}
    val label=when(kind) {"trip"->"wyjazd";"event","convert"->"punkt planu";"task"->"zadanie";"proposal"->"propozycję";else->"wydatek"}
    Page(if(existing==null || kind=="convert") "Dodaj $label" else "Edytuj $label",if(kind=="convert") "Propozycja zostanie przeniesiona do harmonogramu." else "Zmiany zapiszemy również bez internetu.") {
        Panel {
            Field("Nazwa",title,{title=it})
            when(kind) {
                "trip" -> {
                    Field("Miejsce docelowe",destination,{destination=it})
                    Field("Początek (RRRR-MM-DD)",date,{date=it})
                    Field("Koniec (RRRR-MM-DD)",endDate,{endDate=it})
                    Choice("Waluta wyjazdu",currency,listOf("PLN" to "PLN - polski złoty","EUR" to "EUR - euro","USD" to "USD - dolar")) {currency=it}
                    Field("Opis",description,{description=it},multiline=true)
                }
                "event","convert" -> {
                    Field("Data (RRRR-MM-DD)",date,{date=it})
                    Field("Godzina (GG:MM)",time,{time=it})
                    Field("Miejsce",place,{place=it})
                    Field("Opis",description,{description=it},multiline=true)
                    Row(verticalAlignment=Alignment.CenterVertically) {Text("Przypomnij o tej godzinie",Modifier.weight(1f));Switch(reminder,onCheckedChange={reminder=it})}
                    if(reminder) Text("Powiadomienie lokalne może być opóźnione przez oszczędzanie baterii.",style=MaterialTheme.typography.bodySmall)
                }
                "task" -> {
                    Choice("Lista",category,listOf("task" to "Zadania","packing" to "Pakowanie")) {category=it}
                    Choice("Osoba odpowiedzialna",assignee,listOf("" to "Bez przypisania")+members.map {it.userId to it.name}) {assignee=it}
                    Row(verticalAlignment=Alignment.CenterVertically) {Text("Przypomnienie",Modifier.weight(1f));Switch(reminder,onCheckedChange={reminder=it})}
                    if(reminder) {Field("Data (RRRR-MM-DD)",date,{date=it});Field("Godzina (GG:MM)",time,{time=it})}
                }
                "proposal" -> {Field("Miejsce",place,{place=it});Field("Opis",description,{description=it},multiline=true)}
                "expense" -> {
                    Field("Kwota (${trip.currency})",amount,{amount=it},keyboard=KeyboardType.Decimal)
                    Field("Data (RRRR-MM-DD)",date,{date=it})
                    Choice("Kto zapłacił?",payer,members.map {it.userId to it.name}) {payer=it}
                    Row(verticalAlignment=Alignment.CenterVertically) {Text("Własne kwoty udziałów",Modifier.weight(1f));Switch(custom,onCheckedChange={custom=it})}
                    Text(if(custom) "Udziały muszą sumować się do kwoty wydatku." else "Koszt podzielimy równo; resztę groszy rozdzielimy automatycznie.",style=MaterialTheme.typography.bodySmall)
                    members.forEachIndexed {index,m ->
                        Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(m.userId in selected,modifier=Modifier.semantics {contentDescription="Udział w koszcie: ${m.name}"},onCheckedChange={selected=if(it) selected+m.userId else selected-m.userId});Text(m.name,Modifier.weight(1f))}
                        if(custom && m.userId in selected) Field("Udział: ${m.name}",sharesText.getOrElse(index) {"0.00"},{value -> sharesText=sharesText.toMutableList().apply {while(size<=index) add("0.00");this[index]=value}},keyboard=KeyboardType.Decimal)
                    }
                }
            }
        }
        Button(onClick={
            vm.action {
                Validation.title(title.trim())
                when(kind) {
                    "trip" -> {
                        val value=TripData(title.trim(),destination.trim(),date,endDate,currency,description.trim());Validation.trip(value)
                        if(existing==null) {val id=vm.repo.createTrip(value);nav.popBackStack();nav.navigate("trip/$id/overview")} else {
                            require(vm.owner(tripId)) {"Tylko organizator może zmienić wyjazd."}
                            val es=vm.records(tripId,"event").map {vm.decode(it,EventData::class.java)}
                            require(es.all {Validation.date(it.date) in Validation.date(date)..Validation.date(endDate)}) {"Nowe daty muszą obejmować punkty planu."}
                            require(currency==trip.currency || vm.records(tripId,"expense").isEmpty()) {"Nie można zmienić waluty po dodaniu wydatków."}
                            vm.repo.save(tripId,"trip",value,recordId);nav.popBackStack()
                        }
                    }
                    "event","convert" -> {
                        val value=EventData(title.trim(),date,time,place.trim(),description.trim(),oldEvent.done,oldEvent.rank,reminder);Validation.event(value,trip)
                        if(reminder) {require(LocalDateTime.parse("${date}T${time}").isAfter(LocalDateTime.now())) {"Przypomnienie musi być w przyszłości."};requestPermission()}
                        if(kind=="convert") vm.repo.convertProposal(existing!!,value) else if(existing==null) vm.repo.save(tripId,"event",value) else vm.repo.save(tripId,"event",value,recordId)
                        nav.popBackStack()
                    }
                    "task" -> {
                        if(reminder) {Validation.date(date);Validation.time(time);require(LocalDateTime.parse("${date}T${time}").isAfter(LocalDateTime.now())) {"Przypomnienie musi być w przyszłości."};requestPermission()}
                        require(assignee.isBlank() || members.any {it.userId==assignee}) {"Wybierz aktualnego uczestnika."}
                        val value=TaskData(title.trim(),category,assignee,oldTask.done,date,time,reminder)
                        if(existing==null) vm.repo.save(tripId,"task",value) else vm.repo.save(tripId,"task",value,recordId)
                        nav.popBackStack()
                    }
                    "proposal" -> {
                        require(place.length<=240 && description.length<=2000) {"Miejsce lub opis są zbyt długie."}
                        val value=ProposalData(title.trim(),place.trim(),description.trim())
                        if(existing==null) vm.repo.save(tripId,"proposal",value) else vm.repo.save(tripId,"proposal",value,recordId)
                        nav.popBackStack()
                    }
                    "expense" -> {
                        val value=Money.parse(amount);Validation.date(date)
                        require(members.any {it.userId==payer}) {"Wybierz płatnika."}
                        require(selected.isNotEmpty() && selected.all {id -> members.any {it.userId==id}}) {"Wybierz uczestników kosztu."}
                        val split=if(custom) selected.associateWith {id -> val input=sharesText[members.indexOfFirst {it.userId==id}].trim().replace(',','.');if(input in listOf("0","0.0","0.00")) 0L else Money.parse(input)} else Money.equalSplit(value,selected)
                        require(split.values.sum()==value) {"Suma udziałów musi być równa kwocie wydatku."}
                        val expense=ExpenseData(title.trim(),value,payer,split,date)
                        if(existing==null) vm.repo.save(tripId,"expense",expense) else vm.repo.save(tripId,"expense",expense,recordId)
                        nav.popBackStack()
                    }
                }
            }
        },Modifier.fillMaxWidth(),enabled=!busy) {Icon(Icons.Default.Check,null);Spacer(Modifier.width(8.dp));Text("Zapisz")}
        if(existing!=null && kind !in listOf("trip","convert")) TextButton(onClick={delete=true},Modifier.fillMaxWidth(),enabled=!busy) {Text("Usuń",color=MaterialTheme.colorScheme.error)}
    }
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("Usunąć element?")},text={Text("Usunięcie zostanie przekazane pozostałym uczestnikom po synchronizacji.")},confirmButton={TextButton(onClick={vm.action {vm.repo.save(tripId,kind,vm.repo.gson.fromJson(existing!!.json,com.google.gson.JsonObject::class.java),recordId,true);delete=false;nav.popBackStack()}}) {Text("Usuń")}},dismissButton={TextButton(onClick={delete=false}) {Text("Anuluj")}})
}
