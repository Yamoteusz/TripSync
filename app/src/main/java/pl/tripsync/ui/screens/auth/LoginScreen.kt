@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui.screens.auth

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
fun LoginScreen(vm: TripViewModel) {
    var register by rememberSaveable {mutableStateOf(false)}
    var name by rememberSaveable {mutableStateOf("")}
    var email by rememberSaveable {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var visible by rememberSaveable {mutableStateOf(false)}
    val busy by vm.busy.collectAsStateWithLifecycle()
    Page("Podróżujcie razem","Plan, miejsca i wspólne koszty. Jeden spokojny plan na całą przygodę.") {
        TravelHero("TRIPSYNC", "Więcej świata.\nMniej ustaleń.","Od pierwszego pomysłu do ostatniego rozliczenia.")
        Panel {
            RouteLogo(Modifier.size(62.dp))
            Text(if(register) "Utwórz konto" else "Witaj ponownie",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            if(register) Field("Imię",name,{name=it})
            Field("E-mail",email,{email=it},keyboard=KeyboardType.Email)
            OutlinedTextField(password,onValueChange={password=it},label={Text("Hasło (min. 8 znaków)")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),trailingIcon={IconButton(onClick={visible=!visible}) {Icon(if(visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,if(visible) "Ukryj hasło" else "Pokaż hasło")}})
            Button(onClick={vm.action {vm.repo.auth(name,email,password,register)}},Modifier.fillMaxWidth().heightIn(min=52.dp),enabled=!busy) {Text(if(register) "Zarejestruj się" else "Zaloguj się")}
            TextButton(onClick={register=!register;visible=false},Modifier.fillMaxWidth(),enabled=!busy) {Text(if(register) "Mam już konto" else "Nie mam konta - rejestracja")}
        }
        OutlinedButton(onClick={vm.action {vm.repo.demo()}},Modifier.fillMaxWidth(),enabled=!busy) {Icon(Icons.Default.Explore,null);Spacer(Modifier.width(8.dp));Text("Wypróbuj bez konta")}
        Text("Konto pozwala planować wspólnie z grupą. W trybie demo możesz spokojnie poznać aplikację bez rejestracji.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

