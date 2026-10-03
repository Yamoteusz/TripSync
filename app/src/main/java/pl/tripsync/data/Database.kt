package pl.tripsync.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records", indices = [Index("tripId"), Index("kind")])
data class RecordEntity(@PrimaryKey val id: String, val tripId: String, val kind: String, val json: String, val version: Int = 0, val deleted: Boolean = false, val pending: Boolean = false)
@Entity(tableName = "operations", indices = [Index(value = ["recordId"], unique = true)])
data class OperationEntity(@PrimaryKey val id: String, val recordId: String, val tripId: String, val kind: String, val json: String, val baseVersion: Int, val deleted: Boolean, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "conflicts")
data class ConflictEntity(@PrimaryKey val recordId: String, val serverJson: String, val message: String, val invalid: Boolean = false)

@Dao
interface TripDao {
    @Query("SELECT * FROM records WHERE deleted=0") fun observeRecords(): Flow<List<RecordEntity>>
    @Query("SELECT * FROM operations ORDER BY CASE kind WHEN 'trip' THEN 0 WHEN 'proposal' THEN 1 WHEN 'vote' THEN 3 ELSE 2 END, createdAt") suspend fun operations(): List<OperationEntity>
    @Query("SELECT * FROM records") suspend fun records(): List<RecordEntity>
    @Query("SELECT * FROM records WHERE id=:id") suspend fun record(id: String): RecordEntity?
    @Query("SELECT * FROM operations WHERE recordId=:id") suspend fun operation(id: String): OperationEntity?
    @Query("SELECT * FROM operations") fun observeOperations(): Flow<List<OperationEntity>>
    @Query("SELECT * FROM conflicts") fun observeConflicts(): Flow<List<ConflictEntity>>
    @Query("SELECT * FROM conflicts WHERE recordId=:id") suspend fun conflict(id: String): ConflictEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(record: RecordEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(op: OperationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(conflict: ConflictEntity)
    @Query("DELETE FROM operations WHERE recordId=:id") suspend fun deleteOperation(id: String)
    @Query("DELETE FROM conflicts WHERE recordId=:id") suspend fun deleteConflict(id: String)
    @Query("DELETE FROM records WHERE id=:id") suspend fun deleteRecord(id: String)
    @Query("DELETE FROM records WHERE tripId=:trip") suspend fun deleteTripRecords(trip: String)
    @Query("DELETE FROM conflicts WHERE recordId IN (SELECT id FROM records WHERE tripId=:trip)") suspend fun deleteTripConflicts(trip: String)
    @Query("DELETE FROM operations WHERE tripId=:trip") suspend fun deleteTripOperations(trip: String)
    @Query("DELETE FROM records") suspend fun clearRecords()
    @Query("DELETE FROM operations") suspend fun clearOperations()
    @Query("DELETE FROM conflicts") suspend fun clearConflicts()
}

@Database(entities = [RecordEntity::class, OperationEntity::class, ConflictEntity::class], version = 1, exportSchema = true)
abstract class TripDatabase : RoomDatabase() { abstract fun dao(): TripDao }
