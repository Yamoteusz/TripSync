@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.ideas

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
fun ColumnScope.IdeasContent(vm: TripViewModel, state: AppState, nav: NavHostController, tripId: String, rows: List<RecordEntity>, editor: (String,String)->Unit) {
                AddButton("Zaproponuj atrakcję") {editor("proposal","new")}
                val proposals=rows.filter {it.kind=="proposal"}.sortedByDescending {p -> rows.count {it.kind=="vote" && vm.decode(it,VoteData::class.java).proposalId==p.id}}
                if(proposals.isEmpty()) EmptyState(Icons.Default.Lightbulb,"Co chcecie zobaczyć?","Zbierajcie pomysły i wybierzcie je wspólnym głosowaniem.")
                proposals.forEach {r ->
                    val p=vm.decode(r,ProposalData::class.java)
                    val votes=rows.filter {it.kind=="vote" && vm.decode(it,VoteData::class.java).proposalId==r.id}
                    val voted=votes.any {vm.decode(it,VoteData::class.java).userId==state.session?.user?.id}
                    Panel {
                        Text(p.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        if(p.place.isNotBlank()) Text(p.place,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(p.description.isNotBlank()) Text(p.description)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            FilledTonalButton(onClick={vm.action {val u=state.session!!.user.id;vm.repo.save(tripId,"vote",VoteData(r.id,u),"${r.id}:$u",voted)}}) {Icon(if(voted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null);Spacer(Modifier.width(7.dp));Text("${votes.size} · ${if(voted) "Cofnij" else "Głosuj"}")}
                            Spacer(Modifier.weight(1f));IconButton(onClick={editor("proposal",r.id)}) {Icon(Icons.Default.Edit,"Edytuj propozycję")}
                        }
                        TextButton(onClick={nav.navigate("edit/$tripId/convert/${r.id}")}) {Text("Dodaj do planu")}
                    }
                }
}
