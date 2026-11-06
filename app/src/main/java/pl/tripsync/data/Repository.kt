package pl.tripsync.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import pl.tripsync.domain.*
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

class Repository(val db: TripDatabase, private val prefs: Preferences, private val vault: SessionVault) {
    val gson = Gson()
    val dao = db.dao()
    private val gate = Mutex()
    private val _session = MutableStateFlow(vault.read())
    val session = _session.asStateFlow()
    private val _sync = MutableStateFlow("Dane dostępne offline")
    val sync = _sync.asStateFlow()
    val records = dao.observeRecords()
    val operations = dao.observeOperations()
    val conflicts = dao.observeConflicts()
    var schedule: () -> Unit = {}
    var reminder: suspend (RecordEntity) -> Unit = {}
    var cancelReminders: () -> Unit = {}
    private suspend fun api(): TripApi {
        val url = prefs.url()
        require(url.isNotBlank()) { "Ustaw adres serwera API." }
        return Retrofit.Builder().baseUrl(url).client(OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()).addConverterFactory(GsonConverterFactory.create(gson)).build().create(TripApi::class.java)
    }
    private fun token(): String {
        val s = session.value ?: throw IllegalStateException("Zaloguj się.")
        require(!s.demo) { "Ta funkcja wymaga konta na serwerze. Tryb demonstracyjny działa lokalnie." }
        return "Bearer ${s.token}"
    }
    suspend fun auth(name: String, email: String, password: String, register: Boolean) = gate.withLock {
        require(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim())) { "Podaj poprawny e-mail." }
        require(password.length in 8..128) { "Hasło musi mieć od 8 do 128 znaków." }
        if (register) require(name.isNotBlank() && name.length <= 80) { "Podaj imię (maks. 80 znaków)." }
        val request = AuthRequest(email.trim(), password, name.trim())
        val response = if (register) api().register(request) else api().login(request)
        clearLocal()
        val newSession = Session(response.user, response.token)
        withContext(Dispatchers.IO) { vault.write(newSession) }
        _session.value = newSession
        schedule()
    }
    private suspend fun clearLocal() {
        cancelReminders()
        db.withTransaction { dao.clearConflicts(); dao.clearOperations(); dao.clearRecords() }
    }
    suspend fun logout() = gate.withLock {
        if (_session.value?.demo == false) runCatching { api().logout(token()) }
        clearLocal()
        withContext(Dispatchers.IO) { vault.clear() }
        _session.value = null
        _sync.value = "Dane dostępne offline"
    }
    suspend fun demo() = gate.withLock {
        clearLocal()
        val s = Session(User("demo-anna", "Anna", "anna@demo.local"), demo = true)
        withContext(Dispatchers.IO) { vault.write(s) }
        _session.value = s
        val day = LocalDate.now().plusDays(14)
        val trip = "demo-tatry"
        val members = listOf(MemberData("demo-anna", "Anna", "owner"), MemberData("demo-bartek", "Bartek"), MemberData("demo-celina", "Celina"))
        db.withTransaction {
            dao.put(RecordEntity(trip, trip, "trip", gson.toJson(TripData("Weekend w Tatrach", "Zakopane, Polska", day.toString(), day.plusDays(3).toString(), "PLN", "Trzy osoby, cztery dni i wspólna przygoda. Wszystko w jednym miejscu.")), 1))
            members.forEach { dao.put(RecordEntity("$trip:${it.userId}", trip, "member", gson.toJson(it), 1)) }
            listOf(EventData("Spotkanie na dworcu", day.toString(), "08:30", "Kraków Główny", "Bilety mamy w telefonach.", rank=0), EventData("Spacer nad Morskie Oko", day.plusDays(1).toString(), "09:00", "Palenica Białczańska", "Zabierz wodę i wygodne buty.", rank=1), EventData("Kolacja po szlaku", day.plusDays(1).toString(), "18:30", "Zakopane", rank=2)).forEach { dao.put(RecordEntity(UUID.randomUUID().toString(), trip, "event", gson.toJson(it), 1)) }
            listOf(TaskData("Zarezerwować nocleg", assignee="demo-anna", done=true, date=day.toString()), TaskData("Kupić bilety na pociąg", assignee="demo-bartek", date=day.toString()), TaskData("Apteczka i kurtka przeciwdeszczowa", category="packing", assignee="demo-celina", date=day.toString())).forEach { dao.put(RecordEntity(UUID.randomUUID().toString(), trip, "task", gson.toJson(it), 1)) }
            val proposal = "demo-proposal"
            dao.put(RecordEntity(proposal, trip, "proposal", gson.toJson(ProposalData("Wjazd na Kasprowy Wierch", "Kuźnice", "Opcja na ostatni dzień, jeśli pogoda dopisze.")), 1))
            dao.put(RecordEntity("$proposal:demo-bartek", trip, "vote", gson.toJson(VoteData(proposal, "demo-bartek")), 1))
            listOf(ExpenseData("Nocleg dla trzech osób", 72000, "demo-anna", Money.equalSplit(72000, members.map { it.userId }), day.toString(), "accommodation"), ExpenseData("Bilety kolejowe", 18000, "demo-bartek", Money.equalSplit(18000, members.map { it.userId }), day.toString(), "transport")).forEach { dao.put(RecordEntity(UUID.randomUUID().toString(), trip, "expense", gson.toJson(it), 1)) }
        }
        db.withTransaction {
            dao.put(RecordEntity("budget:$trip", trip, "budget", gson.toJson(BudgetData(180000, mapOf("accommodation" to 90000, "transport" to 30000, "food" to 30000))), 1))
            dao.put(RecordEntity("demo-place", trip, "place", gson.toJson(PlaceData("Morskie Oko", 49.1978, 20.0700, "Punkt na mapie wspólnego wyjazdu")), 1))
            dao.put(RecordEntity("demo-history", trip, "activity", gson.toJson(ActivityData("demo-anna", "Anna", "create", "trip", "Weekend w Tatrach", System.currentTimeMillis())), 1))
        }
        _sync.value = "Tryb demo · dane zapisane na urządzeniu"
    }
    suspend fun save(tripId: String, kind: String, payload: Any, id: String = UUID.randomUUID().toString(), deleted: Boolean = false): String = gate.withLock {
        check(session.value != null) { "Zaloguj się." }
        db.withTransaction { saveInternal(tripId, kind, payload, id, deleted) }
        dao.record(id)?.let { reminder(it) }
        if (session.value?.demo == false) schedule()
        id
    }
    private suspend fun saveInternal(tripId: String, kind: String, payload: Any, id: String, deleted: Boolean) {
        require(dao.conflict(id) == null) { "Najpierw rozwiąż konflikt tego rekordu." }
        val previous = dao.record(id)
        val op = dao.operation(id)
        val json = gson.toJson(payload)
        val demo = session.value?.demo == true
        if (demo && kind != "activity") {
            val title = runCatching { gson.fromJson(json, JsonObject::class.java).get("title")?.asString }.getOrNull() ?: kind
            val actor = session.value!!.user
            dao.put(RecordEntity(UUID.randomUUID().toString(), tripId, "activity", gson.toJson(ActivityData(actor.id, actor.name, if (deleted) "delete" else if (previous == null) "create" else "update", kind, title, System.currentTimeMillis())), 1))
        }
        if (deleted && previous?.version == 0 && op != null) {
            dao.deleteRecord(id); dao.deleteOperation(id)
        } else {
            dao.put(RecordEntity(id, tripId, kind, json, previous?.version ?: 0, deleted, !demo))
            if (!demo) {
                dao.deleteOperation(id)
                val operationKind = if (op?.kind == "convert" && kind == "event") { if (deleted) "proposal" else "convert" } else kind
                dao.put(OperationEntity(UUID.randomUUID().toString(), id, tripId, operationKind, json, op?.baseVersion ?: previous?.version ?: 0, deleted))
            }
        }
    }
    suspend fun createTrip(data: TripData): String = gate.withLock {
        Validation.trip(data)
        val s = session.value ?: error("Zaloguj się.")
        val id = UUID.randomUUID().toString()
        db.withTransaction {
            saveInternal(id, "trip", data, id, false)
            dao.put(RecordEntity("$id:${s.user.id}", id, "member", gson.toJson(MemberData(s.user.id, s.user.name, "owner"))))
        }
        if (!s.demo) schedule()
        id
    }
    suspend fun deleteTrip(id: String) = gate.withLock {
        val trip = dao.record(id) ?: return@withLock
        dao.records().filter { it.tripId == id }.forEach { reminder(it.copy(deleted=true)) }
        db.withTransaction {
            if (trip.version == 0 || session.value?.demo == true) {
                dao.deleteTripConflicts(id); dao.deleteTripOperations(id); dao.deleteTripRecords(id)
            } else {
                saveInternal(id, "trip", gson.fromJson(trip.json, JsonObject::class.java), id, true)
                dao.records().filter { it.tripId == id && it.id != id }.forEach { reminder(it.copy(deleted=true)) }
                dao.deleteTripConflicts(id)
                dao.deleteTripOperations(id)
                dao.put(OperationEntity(UUID.randomUUID().toString(), id, id, "trip", trip.json, trip.version, true))
                dao.records().filter { it.tripId == id && it.id != id }.forEach { dao.put(it.copy(deleted=true, pending=false)) }
            }
        }
        schedule()
    }
    suspend fun convertProposal(proposal: RecordEntity, event: EventData): String = gate.withLock {
        val id = proposal.id
        val current = dao.record(id) ?: error("Pomysł nie jest już dostępny.")
        require(current.kind == "proposal" && !current.deleted) { "Ten pomysł został już przeniesiony lub usunięty." }
        val trip = dao.record(proposal.tripId) ?: error("Wyjazd nie jest już dostępny.")
        Validation.event(event, decode(trip, TripData::class.java))
        db.withTransaction {
            saveInternal(proposal.tripId, "event", event, id, false)
            dao.operation(id)?.let { dao.put(it.copy(kind = "convert")) }
            dao.records().filter { it.kind == "vote" && gson.fromJson(it.json, VoteData::class.java).proposalId == proposal.id }.forEach { dao.deleteOperation(it.id); dao.deleteConflict(it.id); dao.put(it.copy(deleted=true, pending=false)) }
        }
        dao.record(id)?.let { reminder(it) }
        schedule()
        id
    }
    suspend fun resolve(recordId: String, keepLocal: Boolean) = gate.withLock {
        val conflict = dao.conflict(recordId) ?: return@withLock
        val local = dao.record(recordId) ?: return@withLock
        val remote = if (conflict.serverJson.isNotBlank()) gson.fromJson(conflict.serverJson, RemoteRecord::class.java) else null
        require(!keepLocal || (!conflict.invalid && remote?.deleted != true)) { "Tej zmiany nie można wymusić. Przyjmij wersję serwera lub odrzuć zmianę." }
        db.withTransaction {
            dao.deleteConflict(recordId); dao.deleteOperation(recordId)
            if (keepLocal) {
                dao.put(local.copy(version=remote?.version ?: 0, pending=true))
                val kind = if (local.kind == "event" && remote?.kind == "proposal") "convert" else local.kind
                dao.put(OperationEntity(UUID.randomUUID().toString(), recordId, local.tripId, kind, local.json, remote?.version ?: 0, local.deleted))
            } else if (remote != null) dao.put(remote.local()) else dao.deleteRecord(recordId)
        }
        if (!keepLocal && remote?.kind != local.kind) reminder(local.copy(deleted=true))
        reminder(dao.record(recordId) ?: local.copy(deleted=true))
        schedule()
    }
    suspend fun sync(): Boolean = gate.withLock {
        if (session.value == null || session.value?.demo == true) return@withLock true
        _sync.value = "Synchronizacja…"
        try {
            val api = api(); val auth = token()
            val pending = dao.operations().filter { dao.conflict(it.recordId) == null }
            for (batch in pending.chunked(100)) {
                val response = api.push(auth, PushRequest(batch.map { RemoteOperation(it.id, it.recordId, it.tripId, it.kind, gson.fromJson(it.json, JsonObject::class.java), it.baseVersion, it.deleted) }))
                db.withTransaction {
                    response.results.forEach { result ->
                        val op = batch.find { it.id == result.id } ?: return@forEach
                        when (result.status) {
                            "applied" -> { result.record?.let { dao.put(it.local()) }; dao.deleteOperation(op.recordId); dao.deleteConflict(op.recordId) }
                            "conflict" -> dao.put(ConflictEntity(op.recordId, result.record?.let { gson.toJson(it) } ?: "", "Ten rekord zmieniono na innym urządzeniu."))
                            else -> dao.put(ConflictEntity(op.recordId, "", result.message ?: "Serwer odrzucił zmianę.", true))
                        }
                    }
                }
            }
            val snapshot = api.snapshot(auth)
            val remaining = dao.operations().map { it.recordId }.toSet()
            val remoteIds = snapshot.records.map { it.id }.toSet()
            val reminders = mutableListOf<RecordEntity>()
            db.withTransaction {
                val localTrips = dao.records().filter { it.kind == "trip" }
                for (trip in localTrips) {
                    if (trip.id !in snapshot.tripIds && !(trip.version == 0 && dao.operation(trip.id) != null)) {
                        dao.records().filter { it.tripId == trip.id }.forEach { reminders += it.copy(deleted=true) }
                        dao.deleteTripConflicts(trip.id); dao.deleteTripOperations(trip.id); dao.deleteTripRecords(trip.id)
                    }
                }
                snapshot.records.forEach { remote ->
                    val conflict = dao.conflict(remote.id)
                    if (conflict != null && conflict.invalid) dao.put(conflict.copy(serverJson=gson.toJson(remote)))
                    if (remote.id !in remaining) { dao.put(remote.local()); reminders += remote.local() }
                }
                dao.records().filter { it.tripId in snapshot.tripIds && it.id !in remoteIds && !it.pending }.forEach { dao.deleteRecord(it.id); reminders += it.copy(deleted=true) }
            }
            reminders.forEach { reminder(it) }
            _sync.value = if (dao.operations().isEmpty()) "Zsynchronizowano · ${java.time.LocalTime.now().withNano(0)}" else "Niektóre zmiany wymagają uwagi"
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _sync.value = errorMessage(e)
            false
        }
    }
    suspend fun invitation(tripId: String, rotate: Boolean): Invitation = gate.withLock {
        if (rotate) api().rotateInvite(token(), tripId) else api().invite(token(), tripId)
    }
    suspend fun join(code: String): String = gate.withLock {
        require(code.trim().length == 12) { "Kod zaproszenia musi mieć 12 znaków." }
        val result = api().join(token(), JoinRequest(code.trim()))
        schedule()
        result.tripId
    }
    fun <T> decode(record: RecordEntity, clazz: Class<T>): T = gson.fromJson(record.json, clazz)
    private fun RemoteRecord.local() = RecordEntity(id, tripId, kind, gson.toJson(payload), version, deleted, false)
    fun errorMessage(e: Exception): String = when (e) {
        is HttpException -> runCatching { gson.fromJson(e.response()?.errorBody()?.string(), JsonObject::class.java).get("message").asString }.getOrDefault("Błąd serwera (${e.code()}). Spróbuj ponownie.")
        is IOException -> "Brak połączenia z serwerem. Dane są dostępne offline."
        else -> e.message ?: "Nie udało się wykonać operacji."
    }
}
