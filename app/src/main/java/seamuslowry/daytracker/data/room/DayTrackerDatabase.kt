package seamuslowry.daytracker.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import seamuslowry.daytracker.data.room.daos.ItemConfigurationDao
import seamuslowry.daytracker.data.room.daos.ItemDao
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemConfiguration

@Database(entities = [Item::class, ItemConfiguration::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DayTrackerDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun itemConfigurationDao(): ItemConfigurationDao
}
