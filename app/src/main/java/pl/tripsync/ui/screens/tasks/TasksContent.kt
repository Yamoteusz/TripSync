@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.tasks

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
fun ColumnScope.TasksContent(vm: TripViewModel, tripId: String, tasks: List<RecordEntity>, name: (String)->String, editor: (String,String)->Unit) {
                AddButton("Dodaj zadanie lub rzecz") {editor("task","new")}
                if(tasks.isEmpty()) EmptyState(Icons.Default.Checklist,"Wszystko do ogarnięcia","Przydziel zadania i przygotuj wspólną listę pakowania.")
                listOf("task" to "Przed wyjazdem","packing" to "Do spakowania").forEach { (category,label) ->
                    val list=tasks.filter {vm.decode(it,TaskData::class.java).category==category}.sortedBy {vm.decode(it,TaskData::class.java).done}
                    if(list.isNotEmpty()) Text(label,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    list.forEach {r -> val task=vm.decode(r,TaskData::class.java)
                        Panel {Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(task.done,modifier=Modifier.semantics {contentDescription="Wykonanie zadania: ${task.title}"},onCheckedChange={vm.action {vm.repo.save(tripId,"task",task.copy(done=it),r.id)}});Column(Modifier.weight(1f)) {Text(task.title,fontWeight=FontWeight.SemiBold);Text(if(task.assignee.isBlank()) "Bez przypisania" else name(task.assignee),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={editor("task",r.id)}) {Icon(Icons.Default.Edit,"Edytuj zadanie")}};if(task.reminder) Text("Przypomnienie: ${Dates.display(task.date)}, ${task.time}",style=MaterialTheme.typography.bodySmall)}
                    }
                }
}
