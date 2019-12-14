package cc.properton

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cc.properton.models.User
import cc.properton.utils.AppUtils
import cc.properton.utils.PrefManager
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var prefManager: PrefManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefManager = PrefManager(this)
        prefManager.isFirstLaunch = false

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.i("current_user_name", "Currently Signed in as:\n${currentUser.displayName}")

            val metadata = currentUser.metadata
            if (metadata?.creationTimestamp == metadata?.lastSignInTimestamp) {
                //TODO: The user is new, show them a fancy intro screen!
                Toast.makeText(this, "You're New, Welcome to Properton!", Toast.LENGTH_LONG).show()
            } else {
                //TODO: This is an existing user, show them a welcome back screen.
                Toast.makeText(this, "Welcome back to Properton!", Toast.LENGTH_SHORT).show()
            }
        }

        setSupportActionBar(home_toolbar)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val popupMenu = PopupMenu(this@MainActivity, home_profile)
        popupMenu.inflate(R.menu.profile_popup)
        val popupLogin = popupMenu.menu.findItem(R.id.popup_login)
        val popupCurrentUserName = popupMenu.menu.findItem(R.id.popup_current_username)
        if (currentUser != null) {
            popupLogin.title = "Log Out"
            popupCurrentUserName.isEnabled = true

            firestore.collection("users").document(auth.uid!!).get().addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                AppUtils.loadImageWithGlide(this, home_profile, user.profilePicUrl)
                val name = "${user.firstName} ${user.lastName}"

                if (name.isNotEmpty()) {
                    popupCurrentUserName.title = name
                } else {
                    popupCurrentUserName.title = "Setup Profile"
                    popupCurrentUserName.setOnActionExpandListener(object :
                        MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    RegistrationActivity::class.java
                                )
                            )
                            return false
                        }

                        override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean = true

                    })
                }
            }
        } else {
            popupLogin.title = "Login"
            popupCurrentUserName.isEnabled = false
            popupCurrentUserName.title = "Guest"
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.popup_view_profile -> {
                    //Start Profile Activity
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
                R.id.popup_edit_profile -> {
                    startActivity(Intent(this, EditProfileActivity::class.java))
                }
                R.id.popup_login -> {
                    if (currentUser != null) {
                        logout()
                    } else {
                        startActivity(Intent(this, AuthenticationActivity::class.java))
                    }
                }
            }
            true
        }
        home_profile.setOnClickListener {
            popupMenu.show()
        }
        home_notification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun logout() {
        AlertDialog.Builder(this).setTitle("LOG OUT")
            .setMessage("Are you sure you want to sign out of this account?")
            .setPositiveButton("Yes") { _, _ ->
                AuthUI.getInstance().signOut(this).addOnSuccessListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }
}
