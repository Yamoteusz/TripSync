package pl.tripsync.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import pl.tripsync.domain.*
import pl.tripsync.ui.*
import java.time.LocalDate

@Composable
fun SettlementScreen(vm: TripViewModel, state: AppState, nav: NavHostController, tripId: String, recordId: String) {
    val trip = state.records.find { it.id == tripId } ?: return
    val currency = vm.decode(trip, TripData::class.java).currency
    val members = state.records.filter { it.tripId == tripId && it.kind == "member" }.map { vm.decode(it, MemberData::class.java) }
    val existing = state.records.find { it.id == recordId && it.kind == "settlement" }
    val old = existing?.let { vm.decode(it, SettlementData::class.java) } ?: SettlementData(from = state.session?.user?.id ?: "")
    val permitted = old.from == state.session?.user?.id
    var to by rememberSaveable(recordId) { mutableStateOf(old.to) }
    var amount by rememberSaveable(recordId) { mutableStateOf(if (old.amount > 0) java.math.BigDecimal.valueOf(old.amount, 2).toPlainString() else "") }
    var date by rememberSaveable(recordId) { mutableStateOf(old.date) }
    var note by rememberSaveable(recordId) { mutableStateOf(old.note) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Page(if (existing == null) "Zapisz spłatę" else "Edytuj spłatę", "Zapisujesz własną deklarację oddania pieniędzy. Aplikacja nie wykonuje przelewów.") {
        Panel {
            Text("Od: ${members.find { it.userId == old.from }?.name ?: "Były uczestnik"}")
            Choice("Komu oddano pieniądze?", to, members.filter { it.userId != old.from }.map { it.userId to it.name }) { to = it }
            Field("Kwota ($currency)", amount, { amount = it }, keyboard = KeyboardType.Decimal)
            Field("Data spłaty (RRRR-MM-DD)", date, { date = it })
            Field("Notatka", note, { note = it }, multiline = true)
        }
        if (permitted) {
            Button(onClick = { vm.action {
                require(to != old.from && members.any { it.userId == to }) { "Wybierz innego uczestnika." }
                require(Validation.date(date) <= LocalDate.now()) { "Spłata nie może być z przyszłości." }
                require(note.length <= 500) { "Notatka może mieć maks. 500 znaków." }
                val data = SettlementData(old.from, to, Money.parse(amount), date, note.trim())
                if (existing == null) vm.repo.save(tripId, "settlement", data) else vm.repo.save(tripId, "settlement", data, existing.id)
                nav.popBackStack()
            } }, modifier = Modifier.fillMaxWidth()) { Text("Zapisz spłatę") }
            if (existing != null) TextButton(onClick = { deleting = true }, modifier = Modifier.fillMaxWidth()) { Text("Usuń spłatę", color = MaterialTheme.colorScheme.error) }
        } else Text("Tylko osoba, która zapisała własną spłatę, może ją edytować.")
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Usunąć spłatę?") }, text = { Text("Salda zostaną przeliczone ponownie.") }, confirmButton = { TextButton(onClick = { vm.action { vm.repo.save(tripId, "settlement", old, existing!!.id, true); nav.popBackStack() } }) { Text("Usuń") } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("Anuluj") } })
}
