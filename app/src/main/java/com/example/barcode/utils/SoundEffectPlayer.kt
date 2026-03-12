package com.example.barcode.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes

object SoundEffectPlayer {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private val activeStreams = mutableListOf<Int>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        isInitialized = true
    }

    fun load(context: Context, @RawRes resId: Int) {
        if (!isInitialized) init(context)

        if (!soundMap.containsKey(resId)) {
            val soundId = soundPool?.load(context, resId, 1) ?: return
            soundMap[resId] = soundId
        }
    }

    fun play(@RawRes resId: Int, volume: Float = 1.0f): Int {
        val soundId = soundMap[resId] ?: return 0
        val streamId = soundPool?.play(soundId, volume, volume, 1, 0, 1.0f) ?: 0
        if (streamId != 0) {
            activeStreams.add(streamId)
        }
        return streamId
    }

    fun stopAll() {
        activeStreams.forEach { streamId ->
            soundPool?.stop(streamId)
        }
        activeStreams.clear()
    }

    fun pauseAll() {
        soundPool?.autoPause()
    }

    fun resumeAll() {
        soundPool?.autoResume()
    }
}