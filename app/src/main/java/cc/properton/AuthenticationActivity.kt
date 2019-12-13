package cc.properton

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cc.properton.models.User
import com.firebase.ui.auth.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_authentication.*

class AuthenticationActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        startSignup()
        setContentView(R.layout.activity_authentication)
        sign_in_btn.setOnClickListener {
            startSignup()
        }
    }

    private fun startSignup() {
        if (auth.currentUser != null) {
            startMainActivity()
        } else {
//            val authMethodPickerLayout =
//                AuthMethodPickerLayout.Builder(R.layout.layout_signup)
//                    .setPhoneButtonId(R.id.phone_login_btn)
//                    .setTosAndPrivacyPolicyId(R.id.privacy_policy_txt)
//                    .build()
            val phoneAuth = AuthUI.IdpConfig.PhoneBuilder()
                .setDefaultCountryIso("ng")
                .build()

            val authSignupIntent = AuthUI.getInstance().createSignInIntentBuilder()
                .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                //  .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTosAndPrivacyPolicyUrls(
                    "http://properton.xyz",
                    "http://properton.xyz/privacypolicy"
                )
                .setAvailableProviders(arrayListOf(phoneAuth))
                .build()
            startActivityForResult(authSignupIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN && data != null) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                onAuthSuccessful()
            } else {
                if (response?.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    auth_status_txt.text =
                        "Authentication Failed, Please check your internet connection!"
                    Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()
                    return
                }
                Log.e(this.localClassName, "Sign-in error: ", response?.error)
            }
        }
    }

    private fun onAuthSuccessful() {
        val currentUser = auth.currentUser
        auth_status_txt.text = "Authentication Successful! Signed In as ${currentUser?.phoneNumber}"

        val metadata = currentUser?.metadata
        if (metadata?.creationTimestamp == metadata?.lastSignInTimestamp) {
            // Create an empty user object
            FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
                .set(User(auth.uid!!)).addOnSuccessListener {
                    Log.i("OnAuthSuccessful", "User Document Created!")
                }

            sign_in_btn.visibility = View.GONE
            auth_success_action_wrapper.visibility = View.VISIBLE
            back_to_home_btn.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            setup_profile_btn.setOnClickListener {
                val intent = Intent(this, RegistrationActivity::class.java)
                startActivity(intent)
            }
        } else {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}