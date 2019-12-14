package cc.properton

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.properton.models.User
import cc.properton.utils.AppUtils
import cc.properton.utils.PrefManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.layout_edit_profile.*
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var prefManager: PrefManager
    private var dob: Date? = null
    private var selectedPicUri: Uri? = null
    private val TAG = "EditProfileActivity"
    private val READ_IMAGE_RC = 106
    private lateinit var pd: ProgressDialog
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_edit_profile)
        prefManager = PrefManager(this)
        prefManager.isFirstLaunch = false
        pd = ProgressDialog(this)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            setupEditFields()
        }
        edit_profile_nav_back.setOnClickListener {
            finish()
        }
        edit_upload_profile_image.setOnClickListener {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, READ_IMAGE_RC)) {
                val imgReadIntent = Intent(Intent.ACTION_GET_CONTENT)
                imgReadIntent.type = "image/*"
                startActivityForResult(imgReadIntent, READ_IMAGE_RC)
            }
        }
        val dateOfBirth = Calendar.getInstance()
        val dateDialogListener =
            DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->
                dateOfBirth.set(Calendar.YEAR, year)
                dateOfBirth.set(Calendar.MONTH, month)
                dateOfBirth.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                edit_dob.setText(AppUtils.formatDate(dateOfBirth.time, "dd/MM/yyyy"))
                dob = dateOfBirth.time
            }

        edit_dob.setOnClickListener {
            DatePickerDialog(
                this,
                dateDialogListener,
                dateOfBirth.get(Calendar.YEAR),
                dateOfBirth.get(Calendar.MONTH),
                dateOfBirth.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        edit_edit_done_btn.setOnClickListener {
            if (selectedPicUri != null) {
                updateProfilePic()
            }
            finish()
        }
        edit_personal_info_save.setOnClickListener {
            updatePersonalInfo()
        }
        edit_contact_info_save.setOnClickListener {
            updateContactInfo()
        }
        edit_other_info_save.setOnClickListener {
            updateOtherInfo()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupEditFields() {
        firestore.collection("users").document(auth.uid!!).get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            AppUtils.loadImageWithGlide(this, edit_profile_pic, user.profilePicUrl)
            edit_first_name.setText(user.firstName)
            edit_last_name.setText(user.lastName)
            edit_address.setText(user.address)
            edit_email_address.setText(user.emailAddress)
            edit_phone_number.setText(user.phoneNumber)
            edit_occupation.setText(user.occupation)
            edit_dob.setText(AppUtils.formatDate(user.dob, "dd MMM yyyy"))
            edit_monthly_income.setText("NGN ${user.monthlyIncome}")
            edit_family_size.setText(user.familySize)
            when (user.gender) {
                "Male" -> edit_gender_male.isChecked = true
                "Female" -> edit_gender_female.isChecked = true
                "Non Binary" -> edit_gender_non_binary.isChecked = true
            }

//                edit_state_spinner.setText(user.state)
//                edit_lga_spinner.setText(user.lga)
        }
    }

    private fun updateProfilePic() {
        pd.setMessage("Updating Profile Pic")
        pd.show()
        val fileId = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("images/$fileId")
        if (selectedPicUri != null) {
            storageRef.putFile(selectedPicUri!!).addOnSuccessListener {
                Log.i(packageName, "Profile Pic Uploaded Successfully...")
                storageRef.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val imageUrl = it.result
                        firestore.collection("users").document(auth.uid!!)
                            .update("profilePicUrl", imageUrl).addOnSuccessListener {
                                Log.i(packageName, "Firestore updated successfully...")
                            }
                    } else {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "error: Your profile picture was not saved",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    pd.dismiss()
                }
            }
        }
    }

    private fun updatePersonalInfo() {
        val firstName = edit_first_name.text.toString()
        val lastName = edit_last_name.text.toString()
        val gender = when {
            edit_gender_male.isChecked -> "Male"
            edit_gender_female.isChecked -> "Female"
            edit_gender_non_binary.isChecked -> "Non Binary"
            else -> "Non Binary"
        }
        if (edit_email_address.text.isNotEmpty()) {
            if (!AppUtils.isEmailValid(edit_email_address.text.toString())) {
                edit_email_address.setTextColor(
                    ContextCompat.getColor(this, R.color.quantum_red)
                )
                Toast.makeText(this, "Invalid Email Address!", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val userMap = hashMapOf<String, Any>(
            "firstName" to firstName, "lastName" to lastName, "gender" to gender
        )
        pd.setMessage("Updating Personal Info")
        pd.show()

        firestore.collection("users").document(auth.uid!!)
            .update(userMap).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i(packageName, "Personal info updated successfully...")
                    Toast.makeText(this, "Personal Info Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i(packageName, "Personal info updated failed... ${it.exception?.message}")
                    Toast.makeText(this, "Personal Info update failed!", Toast.LENGTH_SHORT).show()
                }
                pd.dismiss()
            }
    }

    private fun updateContactInfo() {
        val address = edit_address.text.toString()
        val state = "" //edit_state_spinner.selectedItem.toString()
        val lga = "" //edit_lga_spinner.selectedItem.toString()
        val phoneNumber = edit_phone_number.text.toString()
        val emailAddress = edit_email_address.text.toString()
        val userMap = hashMapOf<String, Any>(
            "address" to address, "state" to state, "lga" to lga,
            "phoneNumber" to phoneNumber, "emailAddress" to emailAddress
        )
        pd.setMessage("Updating Contact Info")
        pd.show()

        firestore.collection("users").document(auth.uid!!)
            .update(userMap).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i(packageName, "Contact info updated successfully...")
                    Toast.makeText(this, "Personal Info Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i(packageName, "Contact info updated failed... ${it.exception?.message}")
                    Toast.makeText(this, "Contact Info update failed!", Toast.LENGTH_SHORT).show()
                }
                pd.dismiss()
            }
    }

    private fun updateOtherInfo() {
        val occupation = edit_occupation.text.toString()
        val familySize = edit_family_size.text.toString()
        val monthlyIncome = edit_monthly_income.text.toString()
        val userMap = hashMapOf<String, Any>(
            "occupation" to occupation, "familySize" to familySize, "monthlyIncome" to monthlyIncome
        )
        pd.setMessage("Updating Other Info")
        pd.show()

        firestore.collection("users").document(auth.uid!!)
            .update(userMap).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i(packageName, "Other info updated successfully...")
                    Toast.makeText(this, "Personal Info Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i(packageName, "Other info updated failed... ${it.exception?.message}")
                    Toast.makeText(this, "Other Info update failed!", Toast.LENGTH_SHORT).show()
                }
                pd.dismiss()
            }
    }

    private fun hasEmptyField(): Boolean {
        var isEmpty = false

        val state = edit_state_spinner.selectedItem
        val lga = edit_lga_spinner.selectedItem

        if (edit_first_name.text.isEmpty()) {
            edit_first_name_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_first_name_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (edit_last_name.text.isEmpty()) {
            edit_last_name_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_last_name_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (edit_dob.text.isEmpty()) {
            edit_dob_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_dob_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (edit_address.text.isEmpty()) {
            edit_address_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_address_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
//        if (state.isEmpty()) {
//            state_spinner_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
//            isEmpty = true
//        } else {
//            state_spinner_label.setTextColor(
//                ContextCompat.getColor(
//                    this,
//                    android.R.color.tertiary_text_dark
//                )
//            )
//        }
//        if (lga.isEmpty()) {
//            lga_spinner_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
//            isEmpty = true
//        } else {
//            lga_spinner_label.setTextColor(
//                ContextCompat.getColor(
//                    this,
//                    android.R.color.tertiary_text_dark
//                )
//            )
//        }
        if (edit_phone_number.text.isEmpty()) {
            edit_phone_number_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_phone_number_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (edit_occupation.text.isEmpty()) {
            edit_occupation_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            edit_occupation_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }

        return isEmpty
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_IMAGE_RC && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedPicUri = data.data
                AppUtils.loadImageWithGlide(this, edit_profile_pic, selectedPicUri)
                Log.i("Image Upload", selectedPicUri?.path!!)
            }
        }
    }

    private fun checkPermission(permission: String, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted")
                return true
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        permission
                    )
                ) {
                    Toast.makeText(
                        this,
                        "We need this permission for the app to Work properly",
                        Toast.LENGTH_LONG
                    ).show()
                    ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
                }
                Log.v(TAG, "Permission is revoked")
                return false
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted")
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission: " + permissions[0] + "was " + grantResults[0])
            if (requestCode == READ_IMAGE_RC) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, READ_IMAGE_RC)
            }
        }
    }

}