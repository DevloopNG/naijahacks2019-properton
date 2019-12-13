package cc.properton

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.properton.models.User
import cc.properton.utils.AppUtils
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_registration.*
import java.util.*

class RegistrationActivity : AppCompatActivity() {
    private val TAG = "RegistrationActivity"
    private val READ_IMAGE_RC = 106
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var dob: Date? = null
    private var selectedPicUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        apply_btn.setOnClickListener {
            if (privacy_policy_cbx.isChecked) {
                registerUser()
            } else {
                privacy_policy_cbx.setBackgroundColor(
                    ContextCompat.getColor(
                        this@RegistrationActivity,
                        R.color.quantum_red
                    )
                )
            }
        }
        reg_nav_back.setOnClickListener {
            finish()
        }
        privacy_policy_cbx.setOnClickListener {
            privacy_policy_cbx.setBackgroundColor(
                ContextCompat.getColor(
                    this@RegistrationActivity,
                    android.R.color.transparent
                )
            )
        }
        reg_upload_profile_image.setOnClickListener {
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
                dob_edt.setText(AppUtils.formatDate(dateOfBirth.time, "dd/MM/yyyy"))
                dob = dateOfBirth.time
            }

        dob_edt.setOnClickListener {
            DatePickerDialog(
                this,
                dateDialogListener,
                dateOfBirth.get(Calendar.YEAR),
                dateOfBirth.get(Calendar.MONTH),
                dateOfBirth.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun registerUser() {
        if (hasEmptyField()) {
            Toast.makeText(this, "Fill up the required fields!", Toast.LENGTH_SHORT).show()
        } else {
            // Required Fields
            val firstName = first_name_edt.text.toString()
            val lastName = last_name_edt.text.toString()
            val address = address_edt.text.toString()
            val state = "" //state_spinner.selectedItem.toString()
            val lga = "" //lga_spinner.selectedItem.toString()
            val phoneNumber = phone_number_edt.text.toString()
            val occupation = occupation_edt.text.toString()
            val gender = when {
                gender_male.isChecked -> "Male"
                gender_female.isChecked -> "Female"
                gender_non_binary.isChecked -> "Non Binary"
                else -> "Non Binary"
            }
            // Optional Fields
            val emailAddress = email_address_edt.text.toString()
            val familySize = family_size_edt.text.toString()
            val monthlyIncome = monthly_income_edt.text.toString()

            if (email_address_edt.text.isNotEmpty()) {
                if (!AppUtils.isEmailValid(email_address_edt.text.toString())) {
                    email_address_edt.setTextColor(
                        ContextCompat.getColor(this, R.color.quantum_red)
                    )
                    Toast.makeText(this, "Invalid Email Address!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            val user = User(
                auth.uid!!,
                firstName,
                lastName,
                gender,
                address,
                dob,
                state,
                lga,
                occupation,
                phoneNumber,
                emailAddress,
                familySize,
                monthlyIncome,
                ""
            )

            val pd = ProgressDialog(this)
            pd.setMessage("Registration in progress")
            pd.show()
            firestore.collection("users").document(auth.uid!!).set(user).addOnSuccessListener {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_LONG).show()
                pd.dismiss()
                startMainActivity()
            }.addOnFailureListener {
                Toast.makeText(this, "Error: Registration Failed!", Toast.LENGTH_LONG).show()
                Log.i("Registration", it.message!!)
                pd.dismiss()
            }

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
                                this@RegistrationActivity,
                                "error: Your profile picture was not saved",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun hasEmptyField(): Boolean {
        var isEmpty = false

        val state = state_spinner.selectedItem
        val lga = lga_spinner.selectedItem

        if (first_name_edt.text.isEmpty()) {
            first_name_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            first_name_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (last_name_edt.text.isEmpty()) {
            last_name_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            last_name_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (dob_edt.text.isEmpty()) {
            dob_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            dob_label.setTextColor(ContextCompat.getColor(this, android.R.color.tertiary_text_dark))
        }
        if (address_edt.text.isEmpty()) {
            address_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            address_label.setTextColor(
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
        if (phone_number_edt.text.isEmpty()) {
            phone_number_edt_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            phone_number_edt_label.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.tertiary_text_dark
                )
            )
        }
        if (occupation_edt.text.isEmpty()) {
            occupation_label.setTextColor(ContextCompat.getColor(this, R.color.quantum_red))
            isEmpty = true
        } else {
            occupation_label.setTextColor(
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
                AppUtils.loadImageWithGlide(this, reg_profile_pic, selectedPicUri)
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

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
