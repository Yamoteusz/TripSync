@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
fun SettingsScreen(vm: TripViewModel,state: AppState) {
    val savedUrl by vm.url.collectAsStateWithLifecycle()
    val dark by vm.dark.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var url by rememberSaveable(savedUrl) {mutableStateOf(savedUrl)}
    var logout by rememberSaveable {mutableStateOf(false)}
    Page("Ustawienia","TripSync · wersja ${pl.tripsync.BuildConfig.VERSION_NAME}") {
        Panel {
            Text("Wygląd",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Ciemny motyw",Modifier.weight(1f));Switch(dark,onCheckedChange={vm.action {vm.graph.prefs.setDark(it)}})}
        }
        Panel {
            Text("Połączenie z serwerem",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            if(state.session==null) {
                Field("Adres API",url,{url=it},keyboard=KeyboardType.Uri)
                Button(onClick={vm.action {vm.graph.prefs.setUrl(url);vm.message.value="Zapisano adres API."}},enabled=!busy) {Text("Zapisz adres")}
            } else {
                Text(savedUrl.ifBlank {"Adres nie został ustawiony"})
                Text("Zmiana serwera jest dostępna po wylogowaniu, aby chronić dane konta.",style=MaterialTheme.typography.bodySmall)
            }
            Text("Domyślny serwer: tripsync.wisp.uno. Połączenie jest szyfrowane przez HTTPS.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(state.session!=null) Panel {
            Text(state.session.user.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            Text(if(state.session.demo) "Tryb demonstracyjny" else state.session.user.email)
            Text(state.sync,style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={logout=true},enabled=!busy) {Icon(Icons.AutoMirrored.Filled.Logout,null);Spacer(Modifier.width(8.dp));Text("Wyloguj się")}
        }
        Panel {Text("Prywatność",fontWeight=FontWeight.Bold);Text("Dane wyjazdów są przechowywane w prywatnej bazie aplikacji. Sesja jest szyfrowana kluczem Android Keystore. Wylogowanie usuwa dane lokalne; niewysłane zmiany zostaną utracone.",style=MaterialTheme.typography.bodySmall)}
    }
    if(logout) AlertDialog(onDismissRequest={logout=false},title={Text("Wylogować się?")},text={Text("Lokalne dane zostaną usunięte. Niewysłane zmiany: ${state.pending.size}. Dane zapisane na serwerze pozostaną dostępne po zalogowaniu.")},confirmButton={TextButton(onClick={logout=false;vm.logout()}) {Text("Wyloguj")}},dismissButton={TextButton(onClick={logout=false}) {Text("Anuluj")}})
}

