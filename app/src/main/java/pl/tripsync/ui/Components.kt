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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import pl.tripsync.data.RecordEntity
import pl.tripsync.domain.*

@Composable
fun Page(title: String, subtitle: String? = null, positionKey: String? = null, positions: MutableMap<String,Int>? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll=rememberScrollState(initial=positions?.get(positionKey) ?: 0)
    LaunchedEffect(scroll,positionKey) {if(positionKey!=null && positions!=null) snapshotFlow {scroll.value}.collect {positions[positionKey]=it}}
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=840.dp).fillMaxWidth().verticalScroll(scroll).imePadding().padding(horizontal=20.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                subtitle?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.6f)),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String) {
    Panel { Icon(icon,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary); Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold); Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun AddButton(label: String,onClick: ()->Unit) { Button(onClick,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(8.dp)); Text(label) } }

fun share(context: android.content.Context, text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,text) },"Udostępnij")) }

@Composable
fun SyncCard(state: AppState,nav: NavHostController) {
    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) { Icon(if(state.session?.demo==true) Icons.Default.OfflineBolt else if(state.conflicts.isNotEmpty()) Icons.Default.CloudOff else if(state.pending.isNotEmpty()) Icons.Default.CloudUpload else Icons.Default.CloudDone,null,Modifier.size(20.dp)); Text(state.sync,style=MaterialTheme.typography.bodySmall) }
            if(state.pending.isNotEmpty()) Text("Zmiany oczekujące: ${state.pending.size}",style=MaterialTheme.typography.bodySmall)
            if(state.conflicts.isNotEmpty()) TextButton(onClick={nav.navigate("conflicts")}) { Text("Rozwiąż konflikty (${state.conflicts.size})") }
        }
    }
}

@Composable
fun Stat(label: String,value: String,modifier: Modifier) {
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
}


@Composable
fun ErrorNotice(message: String) {
    Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline,null,tint=MaterialTheme.colorScheme.onErrorContainer)
            Text(message,color=MaterialTheme.colorScheme.onErrorContainer,style=MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TravelHero(eyebrow:String,title:String,subtitle:String,modifier:Modifier=Modifier) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=Navy) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color(0xFF315950),radius=size.width*.22f,center=androidx.compose.ui.geometry.Offset(size.width*.92f,size.height*.22f))
                drawCircle(Warm,radius=size.width*.055f,center=androidx.compose.ui.geometry.Offset(size.width*.85f,size.height*.2f))
                val mountains=androidx.compose.ui.graphics.Path().apply {moveTo(size.width*.54f,size.height);lineTo(size.width*.78f,size.height*.48f);lineTo(size.width*.9f,size.height*.75f);lineTo(size.width,size.height*.58f);lineTo(size.width,size.height);close()}
                drawPath(mountains,Color(0xFF244A43))
            }
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(eyebrow.uppercase(java.util.Locale.forLanguageTag("pl")),color=Mint,style=MaterialTheme.typography.labelMedium,letterSpacing=1.4.sp)
                Text(title,style=MaterialTheme.typography.headlineMedium,color=Color.White,modifier=Modifier.fillMaxWidth(.85f))
                Text(subtitle,color=Color(0xFFD7E5DD),style=MaterialTheme.typography.bodyMedium,modifier=Modifier.fillMaxWidth(.85f))
            }
        }
    }
}

@Composable
fun SectionTitle(title:String,subtitle:String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge)
        subtitle?.let {Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable
fun ActionTile(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer) {Icon(icon,null,Modifier.padding(12.dp),tint=MaterialTheme.colorScheme.primary)}
            Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
