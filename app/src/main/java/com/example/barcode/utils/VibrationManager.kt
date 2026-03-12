package com.example.barcode.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class VibrationManager(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun vibrate(duration: Long = 500) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}