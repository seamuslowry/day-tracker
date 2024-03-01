package seamuslowry.daytracker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_configuration")
data class ItemConfiguration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val trackingType: TrackingType = LimitedOptionTrackingType.ONE_TO_TEN,
    val active: Boolean = true,
)
