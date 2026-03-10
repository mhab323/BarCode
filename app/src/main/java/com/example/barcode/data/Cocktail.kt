package com.example.barcode.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Cocktail(
    var cocktailId: String = "",
    var bartenderId: String = "",
    var description: String = "",
    var name: String = "",
    var ingredients: String = "",
    var imageUrl: String = "",

) : Parcelable