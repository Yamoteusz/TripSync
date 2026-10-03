package pl.tripsync

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.tripsync.data.*
import pl.tripsync.domain.EventData
import pl.tripsync.domain.TaskData
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class TripSyncApp : Application() {
    lateinit var graph: AppGraph
    override fun onCreate() { super.onCreate(); graph = AppGraph(this) }
}

class AppGraph(val context: Context) {
    val prefs = Preferences(context)
    val db = Room.databaseBuilder(context, TripDatabase::class.java, "tripsync.db").build()
    val repository = Repository(db, prefs, SessionVault(context))
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val work = WorkManager.getInstance(context)
    init {
        repository.schedule = { scheduleSync() }
        repository.reminder = { scheduleReminder(it) }
        repository.cancelReminders = { work.cancelAllWorkByTag("reminder"); work.cancelUniqueWork("sync-once") }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("tripsync", "Plan i zadania", NotificationManager.IMPORTANCE_DEFAULT))
        work.enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        if (repository.session.value != null) {
            scheduleSync()
            scope.launch { db.dao().records().forEach { scheduleReminder(it) } }
        }
    }
    fun scheduleSync() {
        if (repository.session.value?.demo != false) return
        work.enqueueUniqueWork("sync-once", ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS).build())
    }
    suspend fun scheduleReminder(record: RecordEntity) {
        if (record.kind !in listOf("event", "task")) return
        val enabled: Boolean
        val date: String
        val time: String
        if (record.kind == "event") {
            val e = repository.decode(record, EventData::class.java)
            enabled = e.reminder && !e.done
            date = e.date; time = e.time
        } else {
            val t = repository.decode(record, TaskData::class.java)
            enabled = t.reminder && !t.done
            date = t.date; time = t.time
        }
        val name = "reminder-${record.id}"
        val at = runCatching { LocalDateTime.parse("${date}T${time}").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrDefault(0)
        if (!enabled || record.deleted || at <= System.currentTimeMillis()) { work.cancelUniqueWork(name); return }
        work.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().addTag("reminder").setInputData(workDataOf("recordId" to record.id, "at" to at)).setInitialDelay(at-System.currentTimeMillis(), TimeUnit.MILLISECONDS).build())
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as TripSyncApp).graph.repository
        return if (repo.sync()) Result.success() else if (runAttemptCount < 4) Result.retry() else Result.failure()
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as TripSyncApp).graph
        if (graph.repository.session.value == null) return Result.success()
        val record = graph.db.dao().record(inputData.getString("recordId") ?: return Result.success()) ?: return Result.success()
        if (record.deleted) return Result.success()
        val title: String
        val date: String
        val time: String
        val enabled: Boolean
        when (record.kind) {
            "event" -> { val e = graph.repository.decode(record, EventData::class.java); title = e.title; date = e.date; time = e.time; enabled = e.reminder && !e.done }
            "task" -> { val t = graph.repository.decode(record, TaskData::class.java); title = t.title; date = t.date; time = t.time; enabled = t.reminder && !t.done }
            else -> return Result.success()
        }
        val at = runCatching { LocalDateTime.parse("${date}T${time}").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrDefault(0)
        if (!enabled || at != inputData.getLong("at", -1) || System.currentTimeMillis()-at > 3_600_000) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(applicationContext, record.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, "tripsync").setSmallIcon(R.drawable.ic_logo).setContentTitle(title).setContentText("TripSync · $date, $time").setContentIntent(pending).setAutoCancel(true).build()
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(record.id.hashCode(), notification)
        return Result.success()
    }
}
