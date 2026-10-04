package pl.tripsync.ui.screens.places

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import pl.tripsync.domain.*
import pl.tripsync.ui.*

@Composable
fun PlaceEditorScreen(vm:TripViewModel,state:AppState,nav:NavHostController,tripId:String,recordId:String) {
    if(state.records.none {it.id==tripId}) return
    val existing=state.records.find {it.id==recordId && it.kind=="place"}
    val old=existing?.let {vm.decode(it,PlaceData::class.java)} ?: PlaceData()
    var title by rememberSaveable(recordId) {mutableStateOf(old.title)}
    var latitude by rememberSaveable(recordId) {mutableStateOf(if(existing==null) "" else old.latitude.toString())}
    var longitude by rememberSaveable(recordId) {mutableStateOf(if(existing==null) "" else old.longitude.toString())}
    var description by rememberSaveable(recordId) {mutableStateOf(old.description)}
    var deleting by rememberSaveable {mutableStateOf(false)}
    Page(if(existing==null) "Dodaj miejsce" else "Edytuj miejsce","Współrzędne dziesiętne, np. 49.1978 i 20.0700. Możesz je skopiować z map.") {
        Panel {
            Field("Nazwa miejsca",title,{title=it})
            Field("Szerokość geograficzna",latitude,{latitude=it})
            Field("Długość geograficzna",longitude,{longitude=it})
            Field("Opis miejsca",description,{description=it},multiline=true)
        }
        Button(onClick={vm.action {
            val lat=latitude.trim().replace(',','.').toDoubleOrNull() ?: throw IllegalArgumentException("Podaj poprawną szerokość geograficzną.")
            val lon=longitude.trim().replace(',','.').toDoubleOrNull() ?: throw IllegalArgumentException("Podaj poprawną długość geograficzną.")
            val data=PlaceData(title.trim(),lat,lon,description.trim());Validation.place(data)
            if(existing==null) vm.repo.save(tripId,"place",data) else vm.repo.save(tripId,"place",data,existing.id)
            nav.popBackStack()
        }},modifier=Modifier.fillMaxWidth()) {Text("Zapisz miejsce")}
        if(existing!=null) TextButton(onClick={deleting=true},modifier=Modifier.fillMaxWidth()) {Text("Usuń miejsce",color=MaterialTheme.colorScheme.error)}
    }
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Usunąć miejsce?")},text={Text("Miejsce zostanie usunięte ze wspólnej mapy.")},confirmButton={TextButton(onClick={vm.action {vm.repo.save(tripId,"place",old,existing!!.id,true);nav.popBackStack()}}) {Text("Usuń")}},dismissButton={TextButton(onClick={deleting=false}) {Text("Anuluj")}})
}
