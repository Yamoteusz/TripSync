package pl.tripsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.tripsync.ui.TripSyncRoot
import pl.tripsync.ui.TripViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val code = intent?.data?.takeIf { it.scheme == "tripsync" && it.host == "join" }?.getQueryParameter("code")
        setContent {
            val vm: TripViewModel = viewModel(factory = TripViewModel.Factory((application as TripSyncApp).graph))
            TripSyncRoot(vm, code)
        }
    }
}
