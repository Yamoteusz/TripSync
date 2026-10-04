@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui

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

import pl.tripsync.ui.screens.trips.TripsScreen
import pl.tripsync.ui.screens.trip.TripScreen
import pl.tripsync.ui.screens.auth.LoginScreen
import pl.tripsync.ui.screens.settings.SettingsScreen
import pl.tripsync.ui.screens.editor.EditorScreen
import pl.tripsync.ui.screens.members.MembersScreen
import pl.tripsync.ui.screens.conflicts.ConflictsScreen
import pl.tripsync.ui.screens.budget.BudgetScreen
import pl.tripsync.ui.screens.money.SettlementScreen
import pl.tripsync.ui.screens.activity.ActivityScreen
import pl.tripsync.ui.screens.places.PlacesScreen
import pl.tripsync.ui.screens.places.PlaceEditorScreen

@Composable
fun TripSyncRoot(vm: TripViewModel, inviteCode: String?) {
    val state by vm.state.collectAsStateWithLifecycle()
    val dark by vm.dark.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    TripTheme(dark) {
        if (!state.loaded) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }; return@TripTheme }
        key(state.session?.user?.id ?: "guest") {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: ""
            val tripId = entry?.arguments?.getString("tripId")
            val snackbar = remember { SnackbarHostState() }
            var pendingInvite by rememberSaveable { mutableStateOf(inviteCode ?: "") }
            var joinDialog by rememberSaveable { mutableStateOf(false) }
            val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(state.session?.user?.id) {
                if (state.session?.demo == false && pendingInvite.isNotBlank()) joinDialog=true
            }
            LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value=null } }
            Scaffold(
                snackbarHost={ SnackbarHost(snackbar) },
                topBar={ TopAppBar(title={ Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) { RouteLogo(Modifier.size(28.dp)); Text("TripSync",fontWeight=FontWeight.Bold) } }, navigationIcon={ if(route !in listOf("trips","login")) IconButton(onClick={ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Wstecz") } }, actions={
                    if (state.session != null) IconButton(onClick={ vm.sync() },enabled=!busy) { Icon(Icons.Default.Sync,"Synchronizuj") }
                    if (route != "settings") IconButton(onClick={ nav.navigate("settings") { launchSingleTop=true } }) { Icon(Icons.Default.Settings,"Ustawienia") }
                }) },
                bottomBar={ if(route == "trip/{tripId}/{tab}" && tripId != null) {
                    NavigationBar {
                        listOf(Triple("overview","Wyjazd",Icons.Default.Explore),Triple("plan","Plan",Icons.Default.Event),Triple("tasks","Zadania",Icons.Default.Checklist),Triple("ideas","Pomysły",Icons.Default.Lightbulb),Triple("money","Koszty",Icons.Default.AccountBalanceWallet)).forEach { (tab,label,icon) ->
                            NavigationBarItem(selected=entry?.arguments?.getString("tab")==tab,onClick={ nav.navigate("trip/$tripId/$tab") { popUpTo("trips"); launchSingleTop=true } },icon={ Icon(icon,label) },label={ Text(label) })
                        }
                    }
                } }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    NavHost(nav,startDestination=if(state.session==null) "login" else "trips",modifier=Modifier.weight(1f)) {
                        composable("login") { LoginScreen(vm) }
                        composable("settings") { SettingsScreen(vm, state) }
                        composable("trips") { TripsScreen(vm,state,nav,onJoin={ joinDialog=true }) }
                        composable("trip/{tripId}/{tab}") { e ->
                            TripScreen(vm,state,nav,e.arguments!!.getString("tripId")!!,e.arguments!!.getString("tab")!!)
                        }
                        composable("edit/{tripId}/{kind}/{recordId}") { e ->
                            EditorScreen(vm,state,nav,e.arguments!!.getString("tripId")!!,e.arguments!!.getString("kind")!!,e.arguments!!.getString("recordId")!!) { if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        }
                        composable("members/{tripId}") { e -> MembersScreen(vm,state,e.arguments!!.getString("tripId")!!) }
                        composable("budget/{tripId}") { e -> BudgetScreen(vm,state,nav,e.arguments!!.getString("tripId")!!) }
                        composable("settlement/{tripId}/{recordId}") { e -> SettlementScreen(vm,state,nav,e.arguments!!.getString("tripId")!!,e.arguments!!.getString("recordId")!!) }
                        composable("activity/{tripId}") { e -> ActivityScreen(vm,state,e.arguments!!.getString("tripId")!!) }
                        composable("places/{tripId}") { e -> PlacesScreen(vm,state,nav,e.arguments!!.getString("tripId")!!) }
                        composable("place/{tripId}/{recordId}") { e -> PlaceEditorScreen(vm,state,nav,e.arguments!!.getString("tripId")!!,e.arguments!!.getString("recordId")!!) }
                        composable("conflicts") { ConflictsScreen(vm,state) }
                    }
                }
            }
            if(joinDialog) AlertDialog(onDismissRequest={joinDialog=false},title={Text("Dołącz do wyjazdu")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("Wpisz 12-znakowy kod od organizatora."); OutlinedTextField(value=pendingInvite,onValueChange={pendingInvite=it.uppercase()},label={Text("Kod zaproszenia")},singleLine=true) }},confirmButton={TextButton(enabled=!busy,onClick={vm.action { val id=vm.repo.join(pendingInvite); vm.repo.sync(); joinDialog=false; pendingInvite=""; nav.navigate("trip/$id/overview") }}) {Text("Dołącz")}},dismissButton={TextButton(onClick={joinDialog=false}) {Text("Anuluj")}})
        }
    }
}

