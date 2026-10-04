@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.money

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
fun ColumnScope.MoneyContent(vm: TripViewModel, state: AppState, nav: NavHostController, tripId: String, expenses: List<RecordEntity>, t: TripData, name: (String)->String, editor: (String,String)->Unit) {
                val settlements=state.records.filter { it.tripId==tripId && it.kind=="settlement" }
                val paid=settlements.map { vm.decode(it,SettlementData::class.java) }
                val budget=state.records.find { it.tripId==tripId && it.kind=="budget" }?.let { vm.decode(it,BudgetData::class.java) }
                var filter by rememberSaveable { mutableStateOf("all") }
                OutlinedButton(onClick={nav.navigate("budget/$tripId")},Modifier.fillMaxWidth()) {Text(if(budget==null) "Ustal budżet" else "Zobacz lub zmień budżet")}
                val data=expenses.map {vm.decode(it,ExpenseData::class.java)}
                AddButton("Dodaj wydatek") {editor("expense","new")}
                Stat("Wspólne wydatki",Money.format(data.sumOf {it.amount},t.currency),Modifier.fillMaxWidth())
                if(budget!=null) Panel {
                    val spent=data.sumOf {it.amount}
                    Text("Budżet: ${Money.format(budget.limit,t.currency)}",style=MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress=(spent.toDouble()/budget.limit).toFloat().coerceIn(0f,1f),modifier=Modifier.fillMaxWidth())
                    Text(if(spent>budget.limit) "Przekroczono o ${Money.format(spent-budget.limit,t.currency)}" else "Pozostało ${Money.format(budget.limit-spent,t.currency)}",color=if(spent>budget.limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Panel {
                    Text("Wydatki według kategorii",style=MaterialTheme.typography.titleMedium)
                    val totals=Categories.totals(data)
                    Categories.options.forEach { (id,label) ->
                        val spent=totals[id] ?: 0L
                        val limit=budget?.categoryLimits?.get(id)
                        Text("$label: ${Money.format(spent,t.currency)}${if(limit!=null) " / ${Money.format(limit,t.currency)}" else ""}",color=if(limit!=null && spent>limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
                val balances=Money.balances(data,paid)
                if(balances.isNotEmpty()) Panel {
                    Text("Saldo uczestników",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    balances.forEach { (id,value) -> Row { Text(name(id),Modifier.weight(1f));Text((if(value>0) "+" else "")+Money.format(value,t.currency),color=if(value<0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold) } }
                    Text("Dodatnie saldo: otrzyma zwrot. Ujemne: powinien oddać.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val transfers=Money.settle(data,paid)
                if(transfers.isNotEmpty()) Panel {
                    Text("Proponowane rozliczenie",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    transfers.forEach {Text("${name(it.from)} → ${name(it.to)}: ${Money.format(it.amount,t.currency)}")}
                    Text("Propozycja przelewów, bez realizowania płatności.",style=MaterialTheme.typography.bodySmall)
                }
                AddButton("Zapisz spłatę") {nav.navigate("settlement/$tripId/new")}
                if(settlements.isNotEmpty()) Panel {
                    Text("Zapisane spłaty",style=MaterialTheme.typography.titleMedium)
                    settlements.sortedByDescending {vm.decode(it,SettlementData::class.java).date}.forEach {row ->
                        val p=vm.decode(row,SettlementData::class.java)
                        Text("${name(p.from)} → ${name(p.to)} · ${Money.format(p.amount,t.currency)} · ${p.date}")
                        if(p.note.isNotBlank()) Text(p.note,style=MaterialTheme.typography.bodySmall)
                        if(p.from==state.session?.user?.id) TextButton(onClick={nav.navigate("settlement/$tripId/${row.id}")}) {Text("Edytuj spłatę")}
                    }
                }
                Choice("Filtr kategorii",filter,listOf("all" to "Wszystkie")+Categories.options) {filter=it}
                if(expenses.isEmpty()) EmptyState(Icons.Default.AccountBalanceWallet,"Kto za co zapłacił?","Zapisz pierwszy koszt. TripSync policzy udziały i salda.")
                expenses.filter {filter=="all" || (vm.decode(it,ExpenseData::class.java).category ?: "other")==filter}.sortedByDescending {vm.decode(it,ExpenseData::class.java).date}.forEach {r ->val e=vm.decode(r,ExpenseData::class.java)
                    Panel {Row {Text(e.title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);IconButton(onClick={editor("expense",r.id)},Modifier.size(30.dp)) {Icon(Icons.Default.Edit,"Edytuj wydatek")}};Text(Money.format(e.amount,t.currency),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(Categories.label(e.category),style=MaterialTheme.typography.labelMedium);Text("Zapłacił(a): ${name(e.payer)} · ${e.date}",style=MaterialTheme.typography.bodySmall);Text(e.shares.entries.joinToString(" · ") {"${name(it.key)}: ${Money.format(it.value,t.currency)}"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
}
