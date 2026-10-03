package pl.tripsync.ui.screens.members

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
fun MembersScreen(vm: TripViewModel,state: AppState,tripId: String) {
    val members=state.records.filter {it.tripId==tripId && it.kind=="member"}
    val owner=vm.owner(tripId)
    var code by rememberSaveable {mutableStateOf("")}
    var remove by remember {mutableStateOf<RecordEntity?>(null)}
    val context=LocalContext.current
    Page("Ekipa wyjazdu","Wspólny plan zaczyna się od ludzi.") {
        if(owner && state.session?.demo==false) Panel {
            Text("Zaproszenie",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            if(code.isNotBlank()) {Text(code,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);OutlinedButton(onClick={share(context,"Dołącz do naszego wyjazdu w TripSync!\nKod: $code\ntripsync://join?code=$code")}) {Text("Udostępnij kod i link")}}
            Button(onClick={vm.action {vm.repo.sync();code=vm.repo.invitation(tripId,false).code}}) {Text(if(code.isBlank()) "Pokaż kod zaproszenia" else "Odśwież kod")}
            if(code.isNotBlank()) TextButton(onClick={vm.action {code=vm.repo.invitation(tripId,true).code}}) {Text("Wygeneruj nowy kod")}
            Text("Nowy kod unieważni poprzednie zaproszenie. Zaproszenia wymagają połączenia z serwerem.",style=MaterialTheme.typography.bodySmall)
        } else if(state.session?.demo==true) Text("Przykładowa grupa. Zaproszenia są dostępne po zalogowaniu na konto.",style=MaterialTheme.typography.bodyMedium)
        members.forEach {r -> val m=vm.decode(r,MemberData::class.java)
            Panel {Row(verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Default.Person,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)) {Text(m.name,fontWeight=FontWeight.Bold);Text(if(m.role=="owner") "Organizator" else "Uczestnik",style=MaterialTheme.typography.bodySmall)};if(owner && m.role!="owner") IconButton(onClick={remove=r}) {Icon(Icons.Default.PersonRemove,"Usuń uczestnika")}}}
        }
    }
    remove?.let {r -> AlertDialog(onDismissRequest={remove=null},title={Text("Usunąć uczestnika?")},text={Text("Osoba utraci dostęp po synchronizacji. Najpierw rozlicz lub zmień wydatki, w których uczestniczy.")},confirmButton={TextButton(onClick={vm.action {
        val m=vm.decode(r,MemberData::class.java)
        require(vm.records(tripId,"expense").map {vm.decode(it,ExpenseData::class.java)}.none {it.payer==m.userId || m.userId in it.shares}) {"Najpierw usuń lub zmień wydatki dotyczące tej osoby."}
        vm.repo.save(tripId,"member",m,r.id,true);remove=null
    }}) {Text("Usuń")}},dismissButton={TextButton(onClick={remove=null}) {Text("Anuluj")}}) }
}

