package seamuslowry.daytracker.models

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "item_configuration")
data class ItemConfiguration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val trackingType: TrackingType = LimitedOptionTrackingType.ONE_TO_TEN,
    val active: Boolean = true,
    val highColorArgb: Int? = null,
    val lowColorArgb: Int? = null,
    val orderOverride: Long? = null,
    val lastModified: Instant = Instant.now(),
) : Comparable<ItemConfiguration> {
    override fun compareTo(other: ItemConfiguration) = order.compareTo(other.order)

    val highColor: Color?
        get() = highColorArgb?.let { Color(it) }

    val lowColor: Color?
        get() = lowColorArgb?.let { Color(it) }

    val order: Long
        get() = orderOverride ?: id
}
