package pl.tripsync.ui.screens.budget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import pl.tripsync.domain.*
import pl.tripsync.ui.*

@Composable
fun BudgetScreen(vm: TripViewModel, state: AppState, nav: NavHostController, tripId: String) {
    val trip = state.records.find { it.id == tripId } ?: return
    val currency = vm.decode(trip, TripData::class.java).currency
    val existing = state.records.find { it.tripId == tripId && it.kind == "budget" }
    val old = existing?.let { vm.decode(it, BudgetData::class.java) } ?: BudgetData()
    var total by rememberSaveable(tripId) { mutableStateOf(if (old.limit > 0) java.math.BigDecimal.valueOf(old.limit, 2).toPlainString() else "") }
    var limits by rememberSaveable(tripId) { mutableStateOf(Categories.options.map { old.categoryLimits[it.first]?.let { value -> java.math.BigDecimal.valueOf(value, 2).toPlainString() } ?: "" }) }
    val owner = vm.owner(tripId)
    Page("Budżet wyjazdu", "Łączny limit i limity poszczególnych kategorii w $currency.") {
        if (!owner) Text("Budżet ustala organizator wyjazdu.")
        Panel {
            Field("Łączny budżet", total, { total = it }, keyboard = KeyboardType.Decimal)
            Categories.options.forEachIndexed { index, (_, label) ->
                Field("Limit: $label", limits[index], { value -> limits = limits.toMutableList().apply { this[index] = value } }, keyboard = KeyboardType.Decimal)
            }
            Text("Puste pole oznacza brak limitu kategorii. Suma limitów nie może przekraczać całego budżetu.", style = MaterialTheme.typography.bodySmall)
        }
        if (owner) {
            Button(onClick = { vm.action {
                val categories = Categories.options.mapIndexedNotNull { i, (key, _) -> limits[i].takeIf { it.isNotBlank() }?.let { key to Money.parse(it) } }.toMap()
                val data = BudgetData(Money.parse(total), categories)
                Validation.budget(data)
                vm.repo.save(tripId, "budget", data, "budget:$tripId")
                nav.popBackStack()
            } }, modifier = Modifier.fillMaxWidth()) { Text("Zapisz budżet") }
            if (existing != null) TextButton(onClick = { vm.action { vm.repo.save(tripId, "budget", old, existing.id, true); nav.popBackStack() } }, modifier = Modifier.fillMaxWidth()) { Text("Usuń budżet", color = MaterialTheme.colorScheme.error) }
        }
    }
}
