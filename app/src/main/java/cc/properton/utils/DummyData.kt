package cc.properton.utils

import cc.properton.models.Property
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.round

class DummyData {
    private val titles = arrayListOf(
        "Ajah Housing Estate",
        "3, 4 bedroom Bungalow",
        "Wide Range Duplex",
        "2 Double Decker",
        "5 Single Roof Crib"
    )
    private val locations = arrayListOf(
        "No 13,Cresent way, Ajah Lagos",
        "No 6,Lag tower, Ikeja Lagos",
        "Green road avenue, Ikoyi Lagos",
        "Mile 4,Iyan apaja,Lagos",
        "Off zone 2,Gwarimpa,Abuja"
    )
    private val images = arrayListOf(
        "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcQsGsXWuoxszLd3YJrNHmua4_bFCKRByhgRd-BML2kibqF8GcH0",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcRYgaxDefLXkuJGRRVmztSPHuTvOQ1dHijUp7mfVxjU0y-8PG5a",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcRp0QeJUjPvKRUnjb6Ilj_zRDMaeAVgdU0Oz1If7zHMLGl8Xxk7",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcRg-IonnbX01N56OuXUDqB3-74IsX_XJG_Z7qNfpxm9YIIu-K1W",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcSZv2qpO4o5dOtrLRgxHKvGxnv1EnpzI9ozLiad-oZoChskGuj1"
    )

    fun getProperties(size: Int): ArrayList<Property> {
        val properties = ArrayList<Property>()
        for (i in 0..size) {
            val rand = Random().nextInt(titles.size)
            properties.add(
                Property(
                    "$i",
                    titles[rand],
                    locations[rand],
                    if (rand > size / 2) "co-owning" else "investemt",
                    images[rand],
                    Date(2019, 4, 17),
                    Date(2020, 6, 12),
                    "${i + rand} Million",
                    "${(i + rand) / 2} Million",
                    "$rand"
                )
            )
        }
        return properties
    }
}