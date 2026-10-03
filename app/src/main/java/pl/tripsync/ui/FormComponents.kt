@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui

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

@Composable
fun Field(label: String,value: String,onChange: (String)->Unit,multiline: Boolean=false,keyboard: KeyboardType=KeyboardType.Text) {
    OutlinedTextField(value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=!multiline,minLines=if(multiline) 3 else 1,keyboardOptions=KeyboardOptions(keyboardType=keyboard))
}

@Composable
fun Choice(label: String,value: String,options: List<Pair<String,String>>,onSelect: (String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Column {
        Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick={expanded=true},Modifier.fillMaxWidth()) {Text(options.find {it.first==value}?.second ?: "Wybierz",Modifier.weight(1f));Icon(Icons.Default.ExpandMore,null)}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {options.forEach {(id,name)->DropdownMenuItem(text={Text(name)},onClick={onSelect(id);expanded=false})}}
        }
    }
}

