package cc.properton

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.properton.models.User
import cc.properton.utils.AppUtils
import cc.properton.utils.PrefManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var prefManager: PrefManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        prefManager = PrefManager(this)
        prefManager.isFirstLaunch = false

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(auth.uid!!).get().addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                AppUtils.loadImageWithGlide(this, profile_pic, user.profilePicUrl)
                first_name_tv.text = user.firstName
                last_name_tv.text = user.lastName
                address_tv.text = user.address
                email_address_tv.text = user.emailAddress
                phone_number_tv.text = user.phoneNumber
                occupation_tv.text = user.occupation
                state_tv.text = user.state
                lga_tv.text = user.lga
                dob_tv.text = AppUtils.formatDate(user.dob, "dd MMM yyyy")
                monthly_income_tv.text = "NGN ${user.monthlyIncome}"
                family_size_tv.text = user.familySize
            }
        }
        profile_nav_back.setOnClickListener {
            finish()
        }
        edit_profile_btn.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity()))
        }
    }
}