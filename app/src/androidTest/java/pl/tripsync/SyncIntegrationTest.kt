package pl.tripsync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import pl.tripsync.data.*
import pl.tripsync.domain.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {
    private lateinit var first: Repository
    private lateinit var second: Repository
    private lateinit var trip: String
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() = runBlocking {
        Preferences(context).setUrl("http://10.0.2.2:8080/")
        val id=UUID.randomUUID().toString()
        first=Repository(Room.inMemoryDatabaseBuilder(context,TripDatabase::class.java).build(),Preferences(context),SessionVault(context,"test-first-$id"))
        second=Repository(Room.inMemoryDatabaseBuilder(context,TripDatabase::class.java).build(),Preferences(context),SessionVault(context,"test-second-$id"))
        first.auth("Anna","anna-$id@example.org","Secret123!",true)
        second.auth("Bartek","bartek-$id@example.org","Secret123!",true)
        trip=first.createTrip(TripData("Test synchronizacji","Tatry","2026-10-10","2026-10-13"))
        assertTrue(first.sync())
        val code=first.invitation(trip,false).code
        second.join(code)
        assertTrue(second.sync());assertTrue(first.sync())
    }
    @After fun cleanup() = runBlocking {first.logout();second.logout();first.db.close();second.db.close()}
    @Test fun offlineQueueConflictResolutionAndDeletion() = runBlocking {
        val id=first.save(trip,"event",EventData("Spacer","2026-10-11","09:00"))
        assertEquals(1,first.dao.operations().size)
        assertTrue(first.sync());assertTrue(second.sync())
        first.save(trip,"event",EventData("Zmiana Anny","2026-10-11","10:00"),id)
        second.save(trip,"event",EventData("Zmiana Bartka","2026-10-11","11:00"),id)
        assertTrue(first.sync());assertTrue(second.sync())
        assertNotNull(second.dao.conflict(id))
        assertEquals("Zmiana Bartka",second.decode(second.dao.record(id)!!,EventData::class.java).title)
        second.resolve(id,true);assertTrue(second.sync());assertTrue(first.sync())
        assertEquals("Zmiana Bartka",first.decode(first.dao.record(id)!!,EventData::class.java).title)
        second.save(trip,"event",EventData("Zmiana Bartka","2026-10-11","11:00"),id,true)
        assertTrue(second.sync());assertTrue(first.sync())
        assertTrue(first.dao.record(id)!!.deleted)
        assertTrue(first.dao.operations().isEmpty())
    }
    @Test fun choosingServerVersionDiscardsLocalConflict() = runBlocking {
        val id=first.save(trip,"proposal",ProposalData("Kasprowy"))
        first.sync();second.sync()
        first.save(trip,"proposal",ProposalData("Gubałówka"),id)
        second.save(trip,"proposal",ProposalData("Morskie Oko"),id)
        first.sync();second.sync()
        second.resolve(id,false);second.sync()
        assertEquals("Gubałówka",second.decode(second.dao.record(id)!!,ProposalData::class.java).title)
        assertNull(second.dao.conflict(id));assertTrue(second.dao.operations().isEmpty())
    }
    @Test fun revokedAccessPurgesLocalTrip() = runBlocking {
        val member=first.dao.records().first {it.kind=="member" && first.decode(it,MemberData::class.java).userId==second.session.value!!.user.id}
        first.save(trip,"member",first.decode(member,MemberData::class.java),member.id,true)
        assertTrue(first.sync());assertTrue(second.sync())
        assertFalse(second.dao.records().any {it.tripId==trip})
    }
    @Test fun voteCanBeToggledOverNetwork() = runBlocking {
        val id=first.save(trip,"proposal",ProposalData("Kasprowy"));first.sync();second.sync()
        val user=second.session.value!!.user.id
        val voteId="$id:$user"
        second.save(trip,"vote",VoteData(id,user),voteId);assertTrue(second.sync())
        second.save(trip,"vote",VoteData(id,user),voteId,true);assertTrue(second.sync())
        second.save(trip,"vote",VoteData(id,user),voteId);assertTrue(second.sync())
        assertFalse(second.dao.record(voteId)!!.deleted)
        assertNull(second.dao.conflict(voteId))
    }
    @Test fun budgetsPaymentsPlacesAndAuditSynchronizeBetweenAccounts() = runBlocking {
        val a=first.session.value!!.user.id
        val b=second.session.value!!.user.id
        first.save(trip,"budget",BudgetData(100000,mapOf("food" to 20000)),"budget:$trip")
        first.save(trip,"expense",ExpenseData("Hotel",20000,a,mapOf(a to 10000L,b to 10000L),category="accommodation"))
        first.save(trip,"place",PlaceData("Morskie Oko",49.1978,20.0700))
        assertTrue(first.sync());assertTrue(second.sync())
        assertEquals(100000L,second.decode(second.dao.record("budget:$trip")!!,BudgetData::class.java).limit)
        val id=second.save(trip,"settlement",SettlementData(b,a,10000))
        assertTrue(second.sync());assertTrue(first.sync())
        val rows=first.dao.records().filter {!it.deleted}
        val expenses=rows.filter {it.kind=="expense"}.map {first.decode(it,ExpenseData::class.java)}
        val payments=rows.filter {it.kind=="settlement"}.map {first.decode(it,SettlementData::class.java)}
        assertTrue(Money.settle(expenses,payments).isEmpty())
        assertTrue(rows.any {it.kind=="place"})
        assertTrue(rows.any {it.kind=="activity" && first.decode(it,ActivityData::class.java).actorId==b})
        first.save(trip,"settlement",SettlementData(b,a,10000),id,true)
        assertTrue(first.sync());assertNotNull(first.dao.conflict(id))
        first.resolve(id,false)
    }

    @Test fun simultaneousConversionsKeepOneEventAfterConflictResolution() = runBlocking {
        val id = first.save(trip, "proposal", ProposalData("Wspólny pomysł"))
        assertTrue(first.sync()); assertTrue(second.sync())
        val a = first.dao.record(id)!!
        val b = second.dao.record(id)!!
        assertEquals(id, first.convertProposal(a, EventData("Plan Anny", "2026-10-11", "10:00")))
        assertEquals(id, second.convertProposal(b, EventData("Plan Bartka", "2026-10-11", "11:00")))
        assertTrue(first.sync()); assertTrue(second.sync())
        assertNotNull(second.dao.conflict(id))
        second.resolve(id, true)
        assertTrue(second.sync()); assertTrue(first.sync())
        val events = first.dao.records().filter { it.kind == "event" && !it.deleted }
        assertEquals(1, events.size)
        assertEquals("Plan Bartka", first.decode(events.single(), EventData::class.java).title)
    }
    @Test fun conversionCanBeRetriedAgainstNewerProposal() = runBlocking {
        val id = first.save(trip, "proposal", ProposalData("Pomysł"))
        first.sync(); second.sync()
        val stale = second.dao.record(id)!!
        first.save(trip, "proposal", ProposalData("Nowszy pomysł"), id); first.sync()
        second.convertProposal(stale, EventData("Plan", "2026-10-11", "10:00"))
        second.sync()
        assertNotNull(second.dao.conflict(id))
        second.resolve(id, true)
        assertEquals("convert", second.dao.operation(id)!!.kind)
        assertTrue(second.sync()); assertTrue(first.sync())
        assertEquals("event", first.dao.record(id)!!.kind)
        assertNull(second.dao.conflict(id))
    }
    @Test fun offlineConversionAndFurtherEditingRemainAtomic() = runBlocking {
        val id = first.save(trip, "proposal", ProposalData("Lokalny pomysł"))
        first.convertProposal(first.dao.record(id)!!, EventData("Plan", "2026-10-11", "10:00"))
        first.save(trip, "event", EventData("Plan po edycji", "2026-10-11", "11:00"), id)
        assertEquals("convert", first.dao.operation(id)!!.kind)
        assertEquals(1, first.dao.operations().size)
        assertTrue(first.sync()); assertTrue(second.sync())
        assertEquals("event", second.dao.record(id)!!.kind)
        assertEquals("Plan po edycji", second.decode(second.dao.record(id)!!, EventData::class.java).title)
    }
    @Test fun deletingPendingConversionRemovesOriginalProposal() = runBlocking {
        val id = first.save(trip, "proposal", ProposalData("Pomysł")); first.sync()
        val event = EventData("Plan", "2026-10-11", "10:00")
        first.convertProposal(first.dao.record(id)!!, event)
        first.save(trip, "event", event, id, true)
        assertEquals("proposal", first.dao.operation(id)!!.kind)
        assertTrue(first.sync()); assertTrue(second.sync())
        assertTrue(second.dao.record(id)!!.deleted)
    }
    @Test fun acceptingServerProposalCancelsLocalConversionReminder() = runBlocking {
        val id = first.save(trip, "proposal", ProposalData("Pomysł")); first.sync(); second.sync()
        val cancelled = mutableListOf<RecordEntity>()
        second.reminder = { cancelled += it }
        second.convertProposal(second.dao.record(id)!!, EventData("Plan", "2026-10-11", "10:00", reminder = true))
        first.save(trip, "proposal", ProposalData("Nowszy pomysł"), id); first.sync(); second.sync()
        second.resolve(id, false)
        assertEquals("proposal", second.dao.record(id)!!.kind)
        assertTrue(cancelled.any { it.id == id && it.kind == "event" && it.deleted })
    }
    @Test fun demoConversionSchedulesReminderWithoutNetwork() = runBlocking {
        first.logout(); first.demo()
        val reminders = mutableListOf<RecordEntity>()
        first.reminder = { reminders += it }
        val idea = first.dao.record("demo-proposal")!!
        val date = first.decode(first.dao.record("demo-tatry")!!, TripData::class.java).startDate
        val id = first.convertProposal(idea, EventData("Przypomnienie", date, "10:00", reminder = true))
        assertEquals(idea.id, id)
        assertTrue(first.dao.operations().isEmpty())
        assertEquals(1, reminders.size)
        assertTrue(first.decode(reminders.single(), EventData::class.java).reminder)
    }
}
