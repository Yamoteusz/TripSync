package pl.tripsync

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.tripsync.ui.TripSyncRoot
import pl.tripsync.ui.TripViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val configuration=Configuration(newBase.resources.configuration)
        val locale=Locale.forLanguageTag("pl-PL")
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
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
