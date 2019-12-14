package cc.properton

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cc.properton.utils.PrefManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_payment.*

class PaymentActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var prefManager: PrefManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        prefManager = PrefManager(this)
        prefManager.isFirstLaunch = false

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
    }

    fun onPaymentTabClicked(v: View) {
        when (v.id) {
            R.id.paystack_payment_tab -> {
                direct_bank_layout.visibility = View.GONE
                nhf_layout.visibility = View.GONE
                paystack_layout.visibility = View.VISIBLE
            }
            R.id.nhf_payment_tab -> {
                paystack_layout.visibility = View.GONE
                direct_bank_layout.visibility = View.GONE
                nhf_layout.visibility = View.VISIBLE
            }
            R.id.direct_bank_payment_tab -> {
                paystack_layout.visibility = View.GONE
                nhf_layout.visibility = View.GONE
                direct_bank_layout.visibility = View.VISIBLE
            }
        }
    }
}