package com.example.barcode.data


data class Order(
    var orderId: String = "",
    var eventId: String = "",
    var guestName: String = "",
    var cocktailName: String = "",
    var timestamp: Long = 0L,
    var status: String = "pending",
    var guestImageUrl: String = ""
)