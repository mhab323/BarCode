package com.example.barcode.data

import android.os.Parcelable
import com.example.barcode.model.Cocktail
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Event(
    var eventId: String = "",
    var hostId: String = "",
    var eventName: String = "",
    var location: String = "",
    val numOfGuests: Int = 0,
    var dateString: String = "",
    var timestamp: Long = 0L,
    var timeString: String = "",
    var scheduledTimeMillis: Long = 0L,
    var billingType: String = "",
    var status: String = "upcoming",
    var selectedCocktailIds: List<String> = emptyList(),
    val menu: List<Cocktail> = emptyList(),
    var bartenderIds: List<String> = emptyList()
) : Parcelable