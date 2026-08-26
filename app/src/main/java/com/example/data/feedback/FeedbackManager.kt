package com.example.data.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.model.TripStatus

class FeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        runCatching {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        }
    }

    fun onTripStatusChanged(
        oldStatus: TripStatus,
        newStatus: TripStatus,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean
    ) {
        if (oldStatus == newStatus) return

        if (vibrationEnabled) {
            triggerVibration(newStatus)
        }

        if (soundEnabled) {
            triggerSound(newStatus)
        }
    }

    private fun triggerVibration(status: TripStatus) {
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (status) {
                    TripStatus.MOVING -> VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    TripStatus.WAITING -> VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1)
                    TripStatus.PAUSED -> VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
                    TripStatus.STOPPED -> VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200), -1)
                    TripStatus.NOT_STARTED -> null
                }
                effect?.let { v.vibrate(it) }
            } else {
                @Suppress("DEPRECATION")
                when (status) {
                    TripStatus.MOVING -> v.vibrate(120)
                    TripStatus.WAITING -> v.vibrate(longArrayOf(0, 80, 80, 80), -1)
                    TripStatus.PAUSED -> v.vibrate(longArrayOf(0, 150, 100, 150), -1)
                    TripStatus.STOPPED -> v.vibrate(longArrayOf(0, 100, 80, 200), -1)
                    TripStatus.NOT_STARTED -> {}
                }
            }
        }
    }

    private fun triggerSound(status: TripStatus) {
        runCatching {
            val tone = when (status) {
                TripStatus.MOVING -> ToneGenerator.TONE_PROP_BEEP
                TripStatus.WAITING -> ToneGenerator.TONE_PROP_BEEP2
                TripStatus.PAUSED -> ToneGenerator.TONE_PROP_PROMPT
                TripStatus.STOPPED -> ToneGenerator.TONE_PROP_ACK
                TripStatus.NOT_STARTED -> null
            }
            tone?.let { toneGenerator?.startTone(it, 150) }
        }
    }

    fun release() {
        runCatching {
            toneGenerator?.release()
            toneGenerator = null
        }
    }
}
