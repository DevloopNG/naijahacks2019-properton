package cc.properton.models

import java.util.*

class User(
    val id: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var gender: String = "",
    var address: String = "",
    var dob: Date? = null,
    var state: String = "",
    var lga: String = "",
    var occupation: String = "",
    var phoneNumber: String = "",
    var emailAddress: String = "",
    var familySize: String = "",
    var monthlyIncome: String = "",
    var profilePicUrl: String = ""
)