package cc.properton.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cc.properton.DetailsActivity
import cc.properton.R
import cc.properton.models.Property
import cc.properton.utils.AppUtils
import kotlinx.android.synthetic.main.item_property.view.*
import java.util.*
import kotlin.collections.ArrayList

class PropertyAdapter(private val context: Context, val properties: ArrayList<Property>) :
    RecyclerView.Adapter<PropertyAdapter.PropertyHolder>() {

    inner class PropertyHolder(val iv: View) : RecyclerView.ViewHolder(iv) {
        @SuppressLint("SetTextI18n")
        fun bind(property: Property) {
            iv.item_percentage_raised.text = "NGN ${property.amountRaised}"
            iv.item_property_time_left.text = "${Random().nextInt(365)} days left"
            iv.item_property_title.text = property.title
            iv.item_property_location.text = property.location
            iv.item_property_amount.text = "NGN ${property.totalAmount}"
            iv.item_property_duration.text = "${Random().nextInt(24)} Months"
            iv.item_property_investors.text = "${Random().nextInt(100)}"
            AppUtils.loadImageWithGlide(
                context,
                iv.item_property_image,
                property.imagesUrl,
                R.drawable.house_placeholder_1
            )
            iv.setOnClickListener {
                context.startActivity(Intent(context, DetailsActivity::class.java))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyHolder =
        PropertyHolder(LayoutInflater.from(context).inflate(R.layout.item_property, parent, false))

    override fun getItemCount(): Int = properties.size
    override fun onBindViewHolder(holder: PropertyHolder, position: Int) {
        holder.bind(properties[position])
    }
}