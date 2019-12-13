package cc.properton.utils

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import cc.properton.BuildConfig
import cc.properton.R
import cc.properton.models.User
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


object AppUtils {
    fun isEmailValid(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun formatDate(date: Date?, format: String = "dd MMM yyyy hh:mm a"): String {
        return if (date != null) SimpleDateFormat(format, Locale.getDefault()).format(date)
        else ""

    }

    fun timeSince(date: Date, unitFormat: Int = TimeUnit.FORMAT_FULL): String {
        val creationTime = Date().time - date.time
        val seconds = creationTime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val formattedTime = formatDate(date, "hh:mm a")

        return when {
            // if creationTime less than an hour,return number of minutes.
            seconds < 60 -> {
                "Just now"
            }
            minutes < 60 -> {
                when (unitFormat) {
                    TimeUnit.FORMAT_FULL -> "$minutes ${if (minutes > 1) "minutes" else "minute"}"
                    TimeUnit.FORMAT_ABV -> "$minutes ${if (minutes > 1) "mins" else "min"}"
                    else -> "${minutes}m"
                } + " ago"
            }
            // if creationTime less than a day,return number of hours.
            hours < 24 -> {
                "$formattedTime " + when (unitFormat) {
                    TimeUnit.FORMAT_FULL -> "$hours ${if (hours > 1) "hours" else "hour"}"
                    TimeUnit.FORMAT_ABV -> "$hours ${if (hours > 1) "hrs" else "hr"}"
                    else -> "${hours}h"
                } + " ago"
            }
            // if creationTime less than a 3 days,return number of days.
            days < 5 -> {
                if (days <= 1) {
                    "yesterday $formattedTime"
                } else {
                    when (unitFormat) {
                        TimeUnit.FORMAT_FL -> "${days}d"
                        else -> "$days days"
                    } + " ago"
                }
            }
            // if creationTime less than a year,return time,day and month.
            days < 365 -> formatDate(date, "hh:mm a dd MMM")

            // if creationTime greater than a year,return time,day,month and year.
            else -> formatDate(date, "hh:mm a dd MMM yyyy")
        }
    }

    fun timeSinceCommentOrReply(date: Date): String {
        val creationTime = Date().time - date.time
        val seconds = creationTime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 5 -> "${days}d"
            days < 365 -> formatDate(date, "hh:mm a dd MMM")
            else -> formatDate(date, "hh:mm a dd MMM yy")
        }
    }

    fun loadImageWithGlide(
        context: Context,
        target: ImageView,
        imgUrl: String?,
        placeHolder: Int = R.drawable.profile_pic_placeholder
    ) {
        val rqOptions = RequestOptions().placeholder(placeHolder)
        try {
            Glide.with(context)
                .load(imgUrl)
                .apply(rqOptions)
                .into(target)
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
        }
    }

    fun loadImageWithGlide(
        context: Context,
        target: ImageView,
        imgUri: Uri?,
        placeHolder: Int = R.drawable.profile_pic_placeholder
    ) {
        val rqOptions = RequestOptions().placeholder(placeHolder)
        try {
            Glide.with(context)
                .load(imgUri)
                .apply(rqOptions)
                .into(target)
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
            Log.d(
                AppUtils::class.java.simpleName + "/GLIDE",
                "Can not start load with this context"
            )
        }
    }

    private fun Context?.isAvailable(): Boolean {
        if (this == null) {
            return false
        } else if (this !is Application) {
            if (this is FragmentActivity) {
                return !this.isDestroyed
            } else if (this is Activity) {
                return !this.isDestroyed
            }
        }
        return true
    }

    fun getFullName(user: User): String = "${user.lastName} ${user.firstName}"

    fun getPhoneContacts(context: Context): ArrayList<HashMap<String, String>> {
        val contacts = ArrayList<HashMap<String, String>>()
        val cur = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC "
        )
        while (cur != null && cur.moveToNext()) {
            val name =
                cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber =
                cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val photo =
                cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
            val contact = hashMapOf<String, String>()
            contact["name"] = name
            contact["number"] = phoneNumber
            contact["photoUri"] = photo ?: ""
            contacts.add(contact)
            Log.d(this.javaClass.simpleName + " CONTACT INFO", "Name:$name\nNumber:$phoneNumber")
        }

        cur?.close()
        return contacts
    }

    fun getContactPhoneNumbersOnly(context: Context): ArrayList<String> {
        val numbers = ArrayList<String>()
        for (contact in getPhoneContacts(context)) {
            numbers.add(contact["number"]!!)
        }
        return numbers
    }

    fun sendInvitationSms(context: Context, phoneNumbers: String?) {
        val smsIntent = Intent(Intent.ACTION_VIEW)
        smsIntent.data = Uri.parse("smsto:")
        smsIntent.type = "vnd.android-dir/mms-sms"

        smsIntent.putExtra("address", phoneNumbers)
        smsIntent.putExtra("sms_body", getInvitationText(context))
        context.startActivity(smsIntent)
    }

    fun sendInvitationEmail(context: Context, emails: Array<String>) {
        val subject = "Invest or Co-own with me on Properton"
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")

        emailIntent.putExtra(Intent.EXTRA_EMAIL, emails)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, getInvitationText(context))
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Select Email app..."))
        } catch (anf: ActivityNotFoundException) {
            sendInvitationOthers(context)
        }
    }

    fun sendInvitationWhatsApp(context: Context, phoneNumber: String) {
        val number = phoneNumber.replace(" ", "")
        val phoneNumberWithCountryCode: String =
            if (number.length > 11 && number.startsWith("+234")) {
                number
            } else if (number.length == 11 && number.startsWith("0")) {
                number.replaceFirst("0", "+234")
            } else {
                Toast.makeText(context, "Invalid number or wrong country code", Toast.LENGTH_SHORT)
                    .show()
                return
            }

        val whatsAppIntent = Intent(Intent.ACTION_VIEW)
        try {
            val url =
                "https://api.whatsapp.com/send?phone=$phoneNumberWithCountryCode&text=" + URLEncoder.encode(
                    getInvitationText(context),
                    "UTF-8"
                )
            whatsAppIntent.setPackage("com.whatsapp")
            whatsAppIntent.data = Uri.parse(url)
            if (whatsAppIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsAppIntent)
            } else {
                Toast.makeText(context, "WhatsApp not Found!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendInvitationWhatsApp(context: Context) {
        val whatsAppIntent = Intent(Intent.ACTION_SEND)
        whatsAppIntent.type = "text/plain"
        whatsAppIntent.setPackage("com.whatsapp")
        whatsAppIntent.putExtra(Intent.EXTRA_TEXT, getInvitationText(context))
        if (whatsAppIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(whatsAppIntent)
        } else {
            Toast.makeText(context, "WhatsApp not Found!", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendInvitationOthers(context: Context) {
        val otherIntent = Intent(Intent.ACTION_SEND)
        otherIntent.type = "text/plain"
        otherIntent.putExtra(Intent.EXTRA_TEXT, getInvitationText(context))
        context.startActivity(Intent.createChooser(otherIntent, "Select App to send invite..."))
    }

    fun getInvitationText(context: Context): String {
        val downloadLink = String.format(
            context.getString(R.string.app_playstore_link_template),
            BuildConfig.APPLICATION_ID
        )
        return "Hello,Download the Properton app and Invest or Co-own properties with me.\".\n\nDownload Here: $downloadLink"
    }

    fun hideSoftKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun hideSoftKeyboard(context: Context, view: View?) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    fun hasNetworkConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    fun toggleKeyboardVisibility(context: Context) {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun getProfileCompletion(user: User): Int {
        val firstName = if (user.firstName == null) 0 else 1
        val lastName = if (user.lastName == null) 0 else 1
        val profilePic = if (user.profilePicUrl == null) 0 else 1
        val gender = if (user.gender == null) 0 else 1
        val dateOfBirth = if (user.dob == null) 0 else 1
        val jobPlace = if (user.occupation == null) 0 else 1
        val address = if (user.address == null) 0 else 1
        val phone = if (user.phoneNumber == null) 0 else 1
        val state = if (user.state == null) 0 else 1
        val town = if (user.lga == null) 0 else 1
        val email = if (user.emailAddress == null) 0 else 1

        val completion = firstName + lastName + profilePic + gender +
                dateOfBirth + jobPlace + address + phone +
                state + town + email
        return (completion / 21) * 100
    }

    object TimeUnit {
        const val FORMAT_FULL = 1
        // Abbreviate unit
        const val FORMAT_ABV = 2
        // Use first letter of unit
        const val FORMAT_FL = 3
    }
}