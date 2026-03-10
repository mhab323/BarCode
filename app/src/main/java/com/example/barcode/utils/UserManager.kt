package com.example.barcode.utils

import com.example.barcode.model.User

object UserManager {

    var currentUser: User?= null

    fun clearUser() {
        currentUser = null
    }
}