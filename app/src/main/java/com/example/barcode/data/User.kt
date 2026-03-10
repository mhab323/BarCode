package com.example.barcode.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    var shareCode: String = ""
)