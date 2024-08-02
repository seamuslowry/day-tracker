package seamuslowry.daytracker.data.room.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import seamuslowry.daytracker.models.ItemConfiguration

@Dao
interface ItemConfigurationDao {
    @Query("select * from item_configuration")
    fun getAll(): Flow<List<ItemConfiguration>>

    @Insert
    suspend fun insert(itemConfiguration: ItemConfiguration): Long

    @Update
    suspend fun update(itemConfiguration: ItemConfiguration)

    // need a custom upsert because OnConflictStrategy.REPLACE does a delete and insert
    suspend fun upsert(itemConfiguration: ItemConfiguration): Long {
        if (itemConfiguration.id > 0) {
            update(itemConfiguration)
            return itemConfiguration.id
        }
        return insert(itemConfiguration)
    }

    @Delete
    suspend fun delete(itemConfiguration: ItemConfiguration)
}
