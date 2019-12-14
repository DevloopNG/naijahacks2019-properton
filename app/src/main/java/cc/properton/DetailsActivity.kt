package cc.properton

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.properton.utils.PrefManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var prefManager: PrefManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        prefManager = PrefManager(this)
        prefManager.isFirstLaunch = false
        nav_back.setOnClickListener {
            finish()
        }
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        property_apply_btn.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java))
        }
    }
}