package seamuslowry.daytracker.data.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemWithConfiguration
import java.time.LocalDate

@Dao
interface ItemDao {
    @Query("select * from item")
    fun getAll(): Flow<List<Item>>

    @Query("select min(date) from item")
    fun getEarliestDate(): Flow<LocalDate?>

    @Transaction
    @Query("select * from item where date >= :min and date <= :max")
    fun getFull(min: LocalDate, max: LocalDate): Flow<List<ItemWithConfiguration>>

    @Query("select * from item where date = :date")
    fun get(date: LocalDate): Flow<List<Item>>

    @Transaction
    @Query("select * from item where date = :date")
    fun getFull(date: LocalDate): Flow<List<ItemWithConfiguration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg item: Item)
}
