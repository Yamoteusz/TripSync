@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.plan

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
fun ColumnScope.PlanContent(vm: TripViewModel, tripId: String, events: List<RecordEntity>, editor: (String,String)->Unit) {
                AddButton("Dodaj punkt planu") {editor("event","new")}
                if(events.isEmpty()) EmptyState(Icons.Default.Event,"Zaplanuj pierwszy dzień","Dodaj godzinę, miejsce i opis. Przypomnienia pomogą dotrzeć na czas.")
                events.forEachIndexed {index,r ->
                    val e=vm.decode(r,EventData::class.java)
                    if(index==0 || vm.decode(events[index-1],EventData::class.java).date!=e.date) SectionTitle(Dates.day(e.date),Dates.display(e.date))
                    Panel {
                        Row(verticalAlignment=Alignment.CenterVertically) { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(10.dp)) {Text(e.time,Modifier.padding(10.dp),fontWeight=FontWeight.Bold)};Spacer(Modifier.width(10.dp));Text(if(e.done) "Wykonane" else "Do zrobienia",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant); Checkbox(e.done,modifier=Modifier.semantics {contentDescription="Realizacja punktu: ${e.title}"},onCheckedChange={vm.action {vm.repo.save(tripId,"event",e.copy(done=it),r.id)}}) }
                        Text(e.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        if(e.place.isNotBlank()) Text(e.place,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(e.description.isNotBlank()) Text(e.description,style=MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick={editor("event",r.id)}) {Text("Szczegóły / edytuj")}
                            Spacer(Modifier.weight(1f))
                            IconButton(enabled=index>0 && vm.decode(events[index-1],EventData::class.java).date==e.date,onClick={vm.action {val before=events[index-1]; events.forEachIndexed {i,rr -> val ee=vm.decode(rr,EventData::class.java); val rank=if(rr.id==r.id) index-1 else if(rr.id==before.id) index else i;vm.repo.save(tripId,"event",ee.copy(rank=rank),rr.id)}}}) {Icon(Icons.Default.ArrowUpward,"Przesuń wcześniej")}
                            if(e.reminder) Icon(Icons.Default.NotificationsActive,"Przypomnienie włączone",tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
}
