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

}
