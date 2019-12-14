package cc.properton.models

import java.util.*
import kotlin.collections.ArrayList

data class Property(
    val id: String,
    var title: String = "",
    var location: String = "",
    var type: String = "",
    var imagesUrl: String? = null,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var totalAmount: String = "",
    var amountRaised: String = "",
    var numberOfPeopleNeeded: String = ""
)