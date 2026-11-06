@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.tripsync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
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
    OutlinedTextField(value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=!multiline,minLines=if(multiline) 3 else 1,shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(keyboardType=keyboard,imeAction=if(multiline) androidx.compose.ui.text.input.ImeAction.Default else androidx.compose.ui.text.input.ImeAction.Next))
}

@Composable
fun Choice(label: String,value: String,options: List<Pair<String,String>>,onSelect: (String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Column {
        Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick={expanded=true},Modifier.fillMaxWidth().heightIn(min=56.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {Text(options.find {it.first==value}?.second ?: "Wybierz",Modifier.weight(1f));Icon(Icons.Default.ExpandMore,null)}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {options.forEach {(id,name)->DropdownMenuItem(text={Text(name)},onClick={onSelect(id);expanded=false})}}
        }
    }
}


@Composable
private fun PickerField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold)
            }
            Icon(Icons.Default.ExpandMore,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, minimum: String? = null, maximum: String? = null) {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerField(label,Dates.display(value),Icons.Default.CalendarMonth) { open=true }
    if(open) {
        val configuration=LocalConfiguration.current
        val window=LocalWindowInfo.current.containerSize
        val width=with(LocalDensity.current) {window.width.toDp()}
        val height=with(LocalDensity.current) {window.height.toDp()}
        val compact=width<360.dp || height<640.dp || configuration.fontScale>1.2f
        val initial=runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        val lower=minimum?.let {runCatching {LocalDate.parse(it)}.getOrNull()}
        val upper=maximum?.let {runCatching {LocalDate.parse(it)}.getOrNull()}
        val selected=initial.let {if(lower!=null && it<lower) lower else it}.let {if(upper!=null && it>upper) upper else it}
        if(compact) {
            val context=LocalContext.current
            val dark=MaterialTheme.colorScheme.surface.luminance()<.3f
            DisposableEffect(label,value,minimum,maximum) {
                val dialog=android.app.DatePickerDialog(context,if(dark) pl.tripsync.R.style.Theme_TripSync_DatePicker_Dark else pl.tripsync.R.style.Theme_TripSync_DatePicker,{_,year,month,day -> onChange(LocalDate.of(year,month+1,day).toString())},selected.year,selected.monthValue-1,selected.dayOfMonth)
                lower?.let {dialog.datePicker.minDate=it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()}
                upper?.let {dialog.datePicker.maxDate=it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()}
                dialog.setButton(android.content.DialogInterface.BUTTON_POSITIVE,"Wybierz datę",dialog)
                dialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE,"Anuluj",dialog)
                dialog.setOnDismissListener {open=false}
                dialog.show()
                onDispose {dialog.setOnDismissListener(null);dialog.dismiss()}
            }
        } else {
        val allowed:(Long)->Boolean={millis -> val date=LocalDate.parse(Dates.iso(millis));(lower==null || date>=lower) && (upper==null || date<=upper)}
        val picker=rememberDatePickerState(initialSelectedDateMillis=Dates.millis(selected.toString()),yearRange=minOf(1900,selected.year)..maxOf(2100,selected.year))
        DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(enabled=picker.selectedDateMillis?.let(allowed)==true,onClick={picker.selectedDateMillis?.let {onChange(Dates.iso(it))};open=false}) {Text("Wybierz datę")}},dismissButton={TextButton(onClick={open=false}) {Text("Anuluj")}}) {
            DatePicker(state=picker,modifier=Modifier.heightIn(max=(height-180.dp).coerceAtLeast(240.dp)).verticalScroll(rememberScrollState()),dateValidator=allowed,title={Text(label,Modifier.padding(start=24.dp,top=20.dp))},showModeToggle=false)
        }
        }
    }
}

@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    var open by rememberSaveable {mutableStateOf(false)}
    PickerField(label,value,Icons.Default.Schedule) {open=true}
    if(open) {
        val initial=runCatching {java.time.LocalTime.parse(value)}.getOrDefault(java.time.LocalTime.of(9,0))
        var keyboard by rememberSaveable {mutableStateOf(false)}
        val window=LocalWindowInfo.current.containerSize
        val compact=with(LocalDensity.current) {window.height.toDp()<640.dp || window.width.toDp()<360.dp} || LocalConfiguration.current.fontScale>1.2f
        val picker=rememberTimePickerState(initialHour=initial.hour,initialMinute=initial.minute,is24Hour=true)
        AlertDialog(onDismissRequest={open=false},title={Text("Wybierz godzinę")},text={Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {if(keyboard || compact) TimeInput(picker) else TimePicker(picker);if(!compact) TextButton(onClick={keyboard=!keyboard}) {Icon(if(keyboard) Icons.Default.Schedule else Icons.Default.Keyboard,null);Spacer(Modifier.width(8.dp));Text(if(keyboard) "Wybierz na zegarze" else "Wpisz godzinę")}}},confirmButton={TextButton(onClick={onChange(Dates.time(picker.hour,picker.minute));open=false}) {Text("Ustaw godzinę")}},dismissButton={TextButton(onClick={open=false}) {Text("Anuluj")}})
    }
}

@Composable
fun FormBackGuard(formKey: String, draft: List<String>, busy: Boolean, onDiscard: () -> Unit) {
    val original by rememberSaveable(formKey) { mutableStateOf(draft) }
    var discard by rememberSaveable(formKey) { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = busy || draft != original) {
        if (!busy) discard = true
    }
    if (discard) AlertDialog(
        onDismissRequest = { if (!busy) discard = false },
        title = { Text("Odrzucić zmiany?") },
        text = { Text("Ten formularz ma niezapisane zmiany.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { discard = false; onDiscard() }) { Text("Odrzuć zmiany") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { discard = false }) { Text("Zostań") } }
    )
}
