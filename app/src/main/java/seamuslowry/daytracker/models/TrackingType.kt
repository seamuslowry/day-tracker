package seamuslowry.daytracker.models

import androidx.annotation.StringRes
import seamuslowry.daytracker.R

enum class YesNoOption(val value: Int, @StringRes val text: Int, @StringRes val shortText: Int) {
    NO(0, R.string.no, R.string.no_short),
    YES(2, R.string.yes, R.string.yes_short),
}

data class Option(val value: Int, @StringRes val text: Int? = null, @StringRes val shortText: Int? = null)

sealed interface TrackingType

enum class LimitedOptionTrackingType(val options: List<Option>) : TrackingType {
    ONE_TO_TEN((1..10).map { Option(it) }),
    YES_NO(YesNoOption.entries.map { Option(it.value, it.text, it.shortText) }),
}

data object TextEntryTrackingType : TrackingType
