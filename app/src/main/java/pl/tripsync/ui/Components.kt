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

@Composable
fun Page(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=840.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            subtitle?.let { Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String) {
    Panel { Icon(icon,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary); Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold); Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun AddButton(label: String,onClick: ()->Unit) { Button(onClick,Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(8.dp)); Text(label) } }

fun share(context: android.content.Context, text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,text) },"Udostępnij")) }

@Composable
fun SyncCard(state: AppState,nav: NavHostController) {
    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) { Icon(if(state.session?.demo==true) Icons.Default.OfflineBolt else Icons.Default.CloudDone,null,Modifier.size(20.dp)); Text(state.sync,style=MaterialTheme.typography.bodySmall) }
            if(state.pending.isNotEmpty()) Text("Zmiany oczekujące: ${state.pending.size}",style=MaterialTheme.typography.bodySmall)
            if(state.conflicts.isNotEmpty()) TextButton(onClick={nav.navigate("conflicts")}) { Text("Rozwiąż konflikty (${state.conflicts.size})") }
        }
    }
}

@Composable
fun Stat(label: String,value: String,modifier: Modifier) {
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
}
